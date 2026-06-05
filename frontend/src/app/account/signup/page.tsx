import type { Metadata } from 'next';
import { SignupForm } from '@/features/auth/SignupForm';

export const metadata: Metadata = {
  title: 'Sign up — Hishabi',
};

export default function SignupPage() {
  return (
    <main className="flex flex-1 flex-col items-center justify-center p-8">
      <SignupForm />
    </main>
  );
}
