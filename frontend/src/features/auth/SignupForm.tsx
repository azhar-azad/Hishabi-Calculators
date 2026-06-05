'use client';

import { z } from 'zod';
import { useRouter } from 'next/navigation';
import { useState } from 'react';
import { zodResolver } from '@hookform/resolvers/zod';
import { useForm } from 'react-hook-form';
import { ApiError, apiPost } from '@/lib/api';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Label } from '@/components/ui/label';
import { Input } from '@/components/ui/input';
import { Button } from '@/components/ui/button';
import Link from 'next/link';

const signupSchema = z.object({
  email: z.string()
    .min(1, 'Email is required')
    .email('Enter a valid email address'),
  password: z.string()
    .min(8, 'Must be at least 8 characters')
    .max(128, 'Must be at most 128 characters')
    .regex(
      /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)\S+$/,
      'Must contain at least one uppercase letter, one lowercase letter, and one digit',
      ),
});

type SignupValues = z.infer<typeof signupSchema>;

export function SignupForm() {
  const router = useRouter();
  const [serverError, setServerError] = useState<string | null>(null);

  const {
    register,
    handleSubmit,
    formState: { errors, isSubmitting },
  } = useForm<SignupValues>({ resolver: zodResolver(signupSchema), mode: 'onTouched' });

  async function onSubmit(values: SignupValues) {
    setServerError(null);
    try {
      await apiPost('/api/auth/signup', values);
      router.push('/account/login');
    } catch (err) {
      if (err instanceof ApiError && err.status === 409) {
        setServerError('An account with this email already exists.');
      } else if (err instanceof ApiError) {
        const msg = (err.body as Record<string, unknown>)?.message;
        setServerError(typeof msg === 'string' ? msg : 'Signup failed. Please try again.');
      } else {
        setServerError('Signup failed. Please try again.');
      }
    }
  }

  return (
    <Card className="mx-auto w-full max-w-md">
      <CardHeader>
        <CardTitle>Create an account</CardTitle>
      </CardHeader>
      <CardContent>
        <form onSubmit={handleSubmit(onSubmit)} noValidate className="space-y-4">
          <div className="space-y-1">
            <Label htmlFor="email">Email</Label>
            <Input
              id="email"
              type="email"
              autoComplete="email"
              {...register('email')}
            />
            {errors.email && (
              <p aria-live="polite" className="text-sm text-red-500">{errors.email.message}</p>
            )}
          </div>

          <div className="space-y-1">
            <Label htmlFor="password">Password</Label>
            <Input
              id="password"
              type="password"
              autoComplete="new-password"
              {...register('password')}
            />
            {errors.password && (
              <p aria-live="polite" className="text-sm text-red-500">{errors.password.message}</p>
            )}
          </div>

          {serverError && (
            <p role="alert" className="text-sm text-red-500">
              {serverError}
            </p>
          )}

          <Button type="submit" className="w-full" disabled={isSubmitting}>
            {isSubmitting ? 'Creating account…' : 'Sign up'}
          </Button>

          <p className="text-center text-sm">
            Already have an account?{' '}
            <Link href="/account/login" className="underline">
              Log in
            </Link>
          </p>
        </form>
      </CardContent>
    </Card>
  );
}