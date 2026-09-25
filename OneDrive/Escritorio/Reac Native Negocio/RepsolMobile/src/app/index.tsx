import { Redirect } from 'expo-router';
import { useApp } from '../models/AppContext';

export default function Index() {
  const { status, user } = useApp();
  return <Redirect href={status === "authenticated" ? (user?.role === "admin" ? "/tabs" : "/tabs/stock") : "/login"} />;
}
