const { test } = require('node:test');
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const ts = require('typescript');

// Compile in memory: no test framework dependency or generated project files.
function load(file, imports = {}, globals = {}) {
  const source = fs.readFileSync(path.join(__dirname, '..', file), 'utf8');
  const { outputText } = ts.transpileModule(source, {
    compilerOptions: { module: ts.ModuleKind.CommonJS, target: ts.ScriptTarget.ES2022 },
  });
  const exports = {};
  vm.runInNewContext(outputText, {
    exports, require: (name) => {
      if (!(name in imports)) throw new Error(`Unexpected import: ${name}`);
      return imports[name];
    }, Headers, AbortController, URL, Error, TypeError, setTimeout, clearTimeout, ...globals,
  }, { filename: file });
  return exports;
}

function client(fetch) {
  const config = load('src/config/api.ts', {}, {
    process: { env: { EXPO_PUBLIC_API_URL: 'https://api.example.test/' } },
  });
  return load('src/services/http.ts', { '../config/api': config }, { fetch });
}

test('login sends the exact credentials contract without an existing Bearer', async () => {
  const http = client(async (url, init) => {
    assert.equal(url, 'https://api.example.test/api/auth/login');
    assert.equal(init.method, 'POST');
    assert.equal(init.headers.get('Authorization'), null);
    assert.equal(init.headers.get('Content-Type'), 'application/json');
    assert.equal(init.credentials, 'omit');
    assert.deepEqual(JSON.parse(init.body), { email: 'a@example.test', password: 'test-only' });
    return Response.json({ accessToken: 'test-token', user: { id: 3, nombre: 'Test', email: 'a@example.test', rol: 'ADMIN' } });
  });
  http.setAccessToken('old-token');
  const auth = load('src/services/auth.ts', { './http': http });
  assert.equal((await auth.login(' a@example.test ', 'test-only')).accessToken, 'test-token');
});

test('me and inventory automatically send Bearer and preserve response fields', async () => {
  const paths = [];
  const http = client(async (url, init) => {
    paths.push(new URL(url).pathname);
    assert.equal(init.headers.get('Authorization'), 'Bearer test-token');
    return Response.json(url.endsWith('/me')
      ? { id: 3, nombre: 'Test', email: 'test@example.test', rol: 'VENDEDOR' }
      : [{ idProducto: 2, codigo: 'TEST', nombre: 'Test', stockActual: 1.25, costoUnitario: 4.5 }]);
  });
  http.setAccessToken('test-token');
  const auth = load('src/services/auth.ts', { './http': http });
  assert.equal((await auth.me()).rol, 'VENDEDOR');
  assert.equal((await http.request('/api/inventory'))[0].stockActual, 1.25);
  assert.deepEqual(paths, ['/api/auth/me', '/api/inventory']);
});

for (const status of [400, 401, 403, 404, 409, 429, 500]) {
  test(`ApiError ${status}: message preserved; only 401 invalidates session`, async () => {
    let invalidations = 0;
    const http = client(async () => Response.json({
      timestamp: '2026-09-25T00:00:00Z', status, error: 'Test', message: 'Backend message', path: '/api/inventory',
    }, { status }));
    http.setAccessToken('test-token');
    http.setUnauthorizedHandler(() => invalidations++);
    await assert.rejects(http.request('/api/inventory'), error => {
      assert.equal(error.status, status);
      assert.equal(error.message, 'Backend message');
      return true;
    });
    assert.equal(invalidations, status === 401 ? 1 : 0);
  });
}

test('a late 401 cannot invalidate a newer session', async () => {
  let finish;
  let invalidations = 0;
  const http = client(() => new Promise(resolve => { finish = resolve; }));
  http.setUnauthorizedHandler(() => invalidations++);
  http.setAccessToken('old-token');
  const pending = http.request('/api/inventory');
  http.setAccessToken('new-token');
  finish(new Response('', { status: 401 }));
  await assert.rejects(pending, error => error.status === 401);
  assert.equal(invalidations, 0);
});

test('network failures do not invalidate the session', async () => {
  const http = client(async () => { throw new TypeError('offline'); });
  let invalidations = 0;
  http.setUnauthorizedHandler(() => invalidations++);
  http.setAccessToken('test-token');
  await assert.rejects(http.request('/api/inventory'), /Spring Boot/);
  assert.equal(invalidations, 0);
});

test('non-JSON errors use status fallback; invalid successful JSON is rejected', async () => {
  const http = client(async () => new Response('<html>Error</html>', { status: 500 }));
  await assert.rejects(http.request('/api/inventory'), error => error.status === 500);
  const malformed = client(async () => new Response('<html>Not API</html>'));
  await assert.rejects(malformed.request('/api/inventory'), /JSON/);
});

test('missing API URL fails before sending credentials', async () => {
  const config = load('src/config/api.ts', {}, { process: { env: {} } });
  assert.throws(() => config.apiUrl('/api/auth/login'), /EXPO_PUBLIC_API_URL/);
});

test('SecureStore operations are ordered: logout removes a pending login token', async () => {
  let token = null;
  const secureStore = {
    WHEN_UNLOCKED_THIS_DEVICE_ONLY: 1,
    getItemAsync: async () => token,
    setItemAsync: async (_key, value, options) => {
      assert.equal(options.keychainAccessible, 1);
      await new Promise(resolve => setTimeout(resolve, 5));
      token = value;
    },
    deleteItemAsync: async () => { token = null; },
  };
  const session = load('src/storage/session.ts', {
    'react-native': { Platform: { OS: 'android' } }, 'expo-secure-store': secureStore,
  });
  const login = session.writeToken('test-token');
  const logout = session.writeToken(null);
  await Promise.all([login, logout]);
  assert.equal(await session.readToken(), null);
});

test('web session remains in memory and never invokes native storage', async () => {
  const session = load('src/storage/session.ts', {
    'react-native': { Platform: { OS: 'web' } }, 'expo-secure-store': {},
  });
  await session.writeToken('test-token');
  assert.equal(await session.readToken(), 'test-token');
  await session.writeToken(null);
  assert.equal(await session.readToken(), null);
});

test('recovery uses exact public endpoints and JSON fields, without Bearer', async () => {
  const calls = [];
  const http = client(async (url, init) => {
    assert.equal(init.method, 'POST');
    assert.equal(init.headers.get('Authorization'), null);
    calls.push({ path: new URL(url).pathname, body: JSON.parse(init.body) });
    return Response.json({ message: 'Generic server confirmation' });
  });
  http.setAccessToken('existing-token');
  const auth = load('src/services/auth.ts', { './http': http });
  assert.equal((await auth.forgotPassword(' test@example.test ')).message, 'Generic server confirmation');
  const token = 'a'.repeat(43);
  await auth.resetPassword(token, 'Test-password-123');
  assert.deepEqual(calls, [
    { path: '/api/auth/forgot-password', body: { email: 'test@example.test' } },
    { path: '/api/auth/reset-password', body: { token, newPassword: 'Test-password-123' } },
  ]);
});

test('only the two supported roles can authenticate or restore a session', async () => {
  for (const role of ['ADMIN', 'VENDEDOR', 'UNKNOWN', 'constructor', null]) {
    const user = { id: 1, nombre: 'Test', email: 'test@example.test', rol: role };
    const http = client(async url => Response.json(url.endsWith('/me') ? user : { accessToken: 'token', user }));
    const auth = load('src/services/auth.ts', { './http': http });
    if (role === 'ADMIN' || role === 'VENDEDOR') {
      assert.equal((await auth.me()).rol, role);
      assert.equal((await auth.login('test@example.test', 'password')).user.rol, role);
    } else {
      await assert.rejects(auth.me(), auth.UnsupportedRoleError);
      await assert.rejects(auth.login('test@example.test', 'password'), auth.UnsupportedRoleError);
    }
  }
});

test('reset token accepts the backend link parameter; rejects missing, repeated and malformed tokens', () => {
  const { recoveryToken } = load('src/services/recovery.ts');
  const token = 'A_b-'.repeat(10) + 'abc';
  assert.equal(recoveryToken(token), token);
  assert.equal(recoveryToken(`https://example.test/reset-password?token=${token}`), token);
  assert.equal(recoveryToken(`repsolmobile://reset-password?token=${token}`), token);
  for (const value of ['', 'abc', 'a'.repeat(44), 'a'.repeat(42), `https://example.test/?token=${token}&token=${token}`, 'https://example.test/']) {
    assert.equal(recoveryToken(value), null);
  }
});

test('public reset failure preserves session and exposes backend validation/rate limits', async () => {
  for (const status of [400, 429, 500]) {
    let invalidated = false;
    const http = client(async () => Response.json({ message: 'Server rejection' }, { status }));
    http.setAccessToken('existing-token');
    http.setUnauthorizedHandler(() => { invalidated = true; });
    const auth = load('src/services/auth.ts', { './http': http });
    await assert.rejects(auth.resetPassword('a'.repeat(43), 'Test-password-123'), error => error.status === status);
    assert.equal(invalidated, false);
  }
});

test('product operations use the verified endpoints, verbs and DTO without stock or sale price', async () => {
  const calls = [];
  const http = client(async (url, init) => {
    assert.equal(init.headers.get('Authorization'), 'Bearer test-token');
    calls.push({ path: new URL(url).pathname, method: init.method ?? 'GET', body: init.body ? JSON.parse(init.body) : null });
    return Response.json({ id: 7 });
  });
  http.setAccessToken('test-token');
  const products = load('src/services/products.ts', { './http': http });
  const body = { codigo: 'TEST', nombre: 'Producto de prueba', descripcion: 'Description', presentacion: 'Unit', volumen: 1.25, unidad: 'L', costoUnitario: 0 };
  await products.listProducts();
  await products.getProduct(7);
  await products.createProduct(body);
  await products.updateProduct(7, body);
  await products.setProductStatus(7, 'INACTIVO');
  await products.setProductStatus(7, 'ACTIVO');
  assert.deepEqual(calls, [
    { path: '/api/products', method: 'GET', body: null },
    { path: '/api/products/7', method: 'GET', body: null },
    { path: '/api/products', method: 'POST', body },
    { path: '/api/products/7', method: 'PUT', body },
    { path: '/api/products/7/status', method: 'PATCH', body: { estado: 'INACTIVO' } },
    { path: '/api/products/7/status', method: 'PATCH', body: { estado: 'ACTIVO' } },
  ]);
});

test('inventory separates stock, kardex, entries and supported adjustments; identity is not sent', async () => {
  const calls = [];
  const http = client(async (url, init) => {
    assert.equal(init.headers.get('Authorization'), 'Bearer test-token');
    calls.push({ path: new URL(url).pathname, method: init.method ?? 'GET', body: init.body ? JSON.parse(init.body) : null });
    return Response.json({ idMovimiento: 5, saldoAcumulado: 3.125 });
  });
  http.setAccessToken('test-token');
  const inventory = load('src/services/inventory.ts', { './http': http });
  await inventory.listInventory();
  await inventory.getStock(7);
  await inventory.getKardex(7);
  const body = { productoId: 7, cantidad: 1.125, documentoOrigen: 'TEST', observacion: null };
  for (const operation of ['entries', 'adjustments/in', 'adjustments/out']) {
    assert.equal((await inventory.registerMovement(operation, body)).saldoAcumulado, 3.125);
  }
  assert.deepEqual(calls, [
    { path: '/api/inventory', method: 'GET', body: null },
    { path: '/api/inventory/7', method: 'GET', body: null },
    { path: '/api/inventory/kardex/7', method: 'GET', body: null },
    ...['entries', 'adjustments/in', 'adjustments/out'].map(operation => ({ path: '/api/inventory/' + operation, method: 'POST', body })),
  ]);
});

test('decimal validation rejects NaN, negative values, excess precision and unsafe rounding', () => {
  const { decimal, resourceId } = load('src/services/product-validation.ts');
  assert.equal(decimal('0', 2, 17, true), 0);
  assert.equal(decimal('1,125', 3, 16), 1.125);
  assert.equal(decimal('12.50', 2, 17, true), 12.5);
  for (const value of ['', 'abc', 'NaN', 'Infinity', '-1', '1e3', '1.123', '99999999999999999']) {
    assert.throws(() => decimal(value, 2, 17, true));
  }
  assert.throws(() => decimal('0', 3, 16));
  assert.equal(resourceId('7'), 7);
  for (const value of [undefined, ['7'], '0', '-1', 'abc', '7.5', '9007199254740993']) assert.throws(() => resourceId(value));
});

test('inventory conflicts are exposed without simulated success or automatic retries', async () => {
  let calls = 0;
  const http = client(async () => { calls++; return Response.json({ message: 'Stock insuficiente.' }, { status: 409 }); });
  const inventory = load('src/services/inventory.ts', { './http': http });
  await assert.rejects(inventory.registerMovement('adjustments/out', { productoId: 7, cantidad: 99, documentoOrigen: null, observacion: null }),
    error => error.status === 409 && error.message === 'Stock insuficiente.');
  assert.equal(calls, 1);
});

test('customers and quotes preserve real IDs and backend request contracts', async () => {
  const calls = [];
  const http = client(async (url, init) => {
    assert.equal(init.headers.get('Authorization'), 'Bearer test-token');
    calls.push({ path: new URL(url).pathname, method: init.method ?? 'GET', body: init.body ? JSON.parse(init.body) : null });
    return Response.json({ id: 91 });
  });
  http.setAccessToken('test-token');
  const customers = load('src/services/customers.ts', { './http': http });
  const quotes = load('src/services/quotes.ts', { './http': http });
  const customer = { identificacion: 'TEST-91', nombre: 'Test', telefono: null, email: null, direccion: null };
  const quote = { clienteId: 91, observacion: null, detalles: [{ productoId: 27, cantidad: 1.125, precioUnitario: 12.50 }, { productoId: 48, cantidad: 2, precioUnitario: 7 }] };
  await customers.listCustomers(); await customers.getCustomer(91);
  await customers.createCustomer(customer); await customers.updateCustomer(91, customer);
  await quotes.listQuotes(); await quotes.getQuote(83); await quotes.createQuote(quote); await quotes.cancelQuote(83);
  assert.deepEqual(calls, [
    { path: '/api/customers', method: 'GET', body: null },
    { path: '/api/customers/91', method: 'GET', body: null },
    { path: '/api/customers', method: 'POST', body: customer },
    { path: '/api/customers/91', method: 'PUT', body: customer },
    { path: '/api/quotes', method: 'GET', body: null },
    { path: '/api/quotes/83', method: 'GET', body: null },
    { path: '/api/quotes', method: 'POST', body: quote },
    { path: '/api/quotes/83/cancel', method: 'PATCH', body: null },
  ]);
});

test('PDF uses the requested quote ID and Bearer and preserves server bytes', async () => {
  const bytes = Buffer.from('%PDF-1.7\nserver document\n');
  const http = client(async (url, init) => {
    assert.equal(new URL(url).pathname, '/api/proformas/83/pdf');
    assert.equal(init.headers.get('Authorization'), 'Bearer test-token');
    assert.equal(init.headers.get('Accept'), 'application/pdf');
    return new Response(bytes, { headers: { 'Content-Type': 'application/pdf' } });
  });
  http.setAccessToken('test-token');
  const quotes = load('src/services/quotes.ts', { './http': http });
  assert.deepEqual(Array.from(await quotes.getQuotePdf(83)), Array.from(bytes));
});

test('PDF rejects HTTP failures and invalid documents; only 401 invalidates session', async () => {
  for (const status of [400, 401, 403, 404, 409, 429, 500]) {
    let invalidated = false;
    const http = client(async () => Response.json({ message: 'Rejected' }, { status }));
    http.setAccessToken('test-token');
    http.setUnauthorizedHandler(() => { invalidated = true; });
    await assert.rejects(http.request('/api/proformas/83/pdf', { responseType: 'pdf' }), error => error.status === status);
    assert.equal(invalidated, status === 401);
  }
  for (const [body, type] of [['<html>Error</html>', 'text/html'], ['broken', 'application/pdf'], ['', 'application/pdf']]) {
    const http = client(async () => new Response(body, { headers: { 'Content-Type': type } }));
    await assert.rejects(http.request('/api/proformas/83/pdf', { responseType: 'pdf' }));
  }
});

test('native PDF shares original bytes and removes cache even when sharing fails', async () => {
  for (const fails of [false, true]) {
    let cached;
    let shared = false;
    const bytes = Uint8Array.from([37, 80, 68, 70, 45, 49]);
    class File {
      constructor(dir, name) { this.uri = dir + name; this.exists = false; cached = this; }
      write(value) { assert.deepEqual(Array.from(value), Array.from(bytes)); this.exists = true; }
      delete() { this.exists = false; }
    }
    const pdf = load('src/services/quote-pdf.ts', {
      'react-native': { Platform: { OS: 'android' } },
      'expo-file-system': { File, Paths: { cache: 'file:///cache/' } },
      'expo-sharing': { isAvailableAsync: async () => true, shareAsync: async uri => {
        assert.match(uri, /proforma-83-\d+\.pdf$/); assert.equal(cached.exists, true); shared = true;
        if (fails) throw new Error('Share failed');
      } },
      'expo-print': { printAsync: async () => { throw new Error('Unexpected print'); } },
      './quotes': { getQuotePdf: async id => { assert.equal(id, 83); return bytes; } },
    });
    if (fails) await assert.rejects(pdf.shareQuotePdf(83), /Share failed/);
    else await pdf.shareQuotePdf(83);
    assert.equal(shared, true); assert.equal(cached.exists, false);
  }
});

test('sales send real IDs and direct details, or only proformaId for conversion', async () => {
  const calls = [];
  const response = { id: 72, clienteId: 91, proformaId: null, total: 35.1, totalAbonado: 0, saldo: 35.1, estado: 'PENDIENTE' };
  const http = client(async (url, init) => {
    assert.equal(init.headers.get('Authorization'), 'Bearer test-token');
    calls.push({ path: new URL(url).pathname, method: init.method ?? 'GET', body: init.body ? JSON.parse(init.body) : null });
    return Response.json(response);
  });
  http.setAccessToken('test-token');
  const sales = load('src/services/sales.ts', { './http': http });
  const direct = { clienteId: 91, detalles: [{ productoId: 27, cantidad: 1.125, precioUnitario: 12.50 }, { productoId: 48, cantidad: 2, precioUnitario: 10.52 }] };
  await sales.listSales();
  assert.equal((await sales.getSale(72)).saldo, 35.1);
  assert.equal((await sales.createSale(direct)).total, 35.1);
  await sales.createSale({ proformaId: 83 });
  await sales.cancelSale(72);
  assert.deepEqual(calls, [
    { path: '/api/sales', method: 'GET', body: null },
    { path: '/api/sales/72', method: 'GET', body: null },
    { path: '/api/sales', method: 'POST', body: direct },
    { path: '/api/sales', method: 'POST', body: { proformaId: 83 } },
    { path: '/api/sales/72/cancel', method: 'PATCH', body: null },
  ]);
});

test('payment creates an immutable receipt for the chosen sale with no invented payment method', async () => {
  const calls = [];
  const http = client(async (url, init) => {
    assert.equal(init.headers.get('Authorization'), 'Bearer test-token');
    calls.push({ path: new URL(url).pathname, method: init.method ?? 'GET', body: init.body ? JSON.parse(init.body) : null });
    return Response.json({ id: 64, ventaId: 72, monto: 10.25 });
  });
  http.setAccessToken('test-token');
  const receipts = load('src/services/receipts.ts', { './http': http });
  const body = { ventaId: 72, monto: 10.25, observacion: 'Test payment' };
  await receipts.listReceipts(); await receipts.getReceipt(64);
  const receipt = await receipts.createReceipt(body);
  assert.equal(receipt.id, 64); assert.equal(receipt.ventaId, 72);
  assert.deepEqual(calls, [
    { path: '/api/receipts', method: 'GET', body: null },
    { path: '/api/receipts/64', method: 'GET', body: null },
    { path: '/api/receipts', method: 'POST', body },
  ]);
});

test('sales and receipts propagate authorization and business conflicts without automatic retries', async () => {
  for (const status of [400, 403, 404, 409, 500]) {
    let calls = 0;
    const http = client(async () => { calls++; return Response.json({ message: 'Backend rejection' }, { status }); });
    const sales = load('src/services/sales.ts', { './http': http });
    const receipts = load('src/services/receipts.ts', { './http': http });
    const rejects = error => error.status === status && error.message === 'Backend rejection';
    await assert.rejects(sales.createSale({ proformaId: 83 }), rejects);
    await assert.rejects(sales.cancelSale(72), rejects);
    await assert.rejects(receipts.createReceipt({ ventaId: 72, monto: 999, observacion: null }), rejects);
    assert.equal(calls, 3);
  }
});

test('dashboard uses backend default period or exact month/year, preserving null metrics', async () => {
  const paths = [];
  const stats = { litrosVendidosMes: null, metaRepsol: null, cumplimientoMeta: null, totalVentasMes: 123.45, totalVentasUSD: 0, saldoPendienteTotal: 23.45, productosStockBajo: null, totalProductos: 4, ventasHoy: 0, recibosHoy: 0 };
  const http = client(async (url, init) => {
    assert.equal(init.headers.get('Authorization'), 'Bearer test-token');
    paths.push(new URL(url).pathname + new URL(url).search);
    return Response.json(stats);
  });
  http.setAccessToken('test-token');
  const { getDashboard } = load('src/services/dashboard.ts', { './http': http });
  assert.equal((await getDashboard()).litrosVendidosMes, null);
  assert.equal((await getDashboard({ year: 2025, month: 2 })).totalVentasMes, 123.45);
  assert.deepEqual(paths, ['/api/dashboard/stats', '/api/dashboard/stats?year=2025&month=2']);
  for (const period of [{ year: 0, month: 1 }, { year: 10000, month: 1 }, { year: 2025, month: 13 }, { year: 2025, month: 0 }, { year: 2025.5, month: 1 }]) assert.throws(() => getDashboard(period));
  assert.equal(paths.length, 2);
});

test('dashboard failures remain failures instead of fabricated zero statistics', async () => {
  for (const status of [400, 401, 403, 500]) {
    const http = client(async () => Response.json({ message: 'Unavailable' }, { status }));
    const { getDashboard } = load('src/services/dashboard.ts', { './http': http });
    await assert.rejects(getDashboard(), error => error.status === status);
  }
});
