import { z } from 'zod'

export const loginSchema = z.object({
  email: z.string().min(1, 'Email é obrigatório').email('Email inválido'),
  password: z.string().min(1, 'Senha é obrigatória'),
})

export type LoginFormValues = z.infer<typeof loginSchema>

// Minimum policy assumed client-side (backend is the source of truth and may
// reject with 400 for weaker passwords per the "Registration with weak
// password" scenario in specs/user-auth/spec.md). Documented as an assumption
// since the spec doesn't pin an exact policy.
export const registerSchema = z
  .object({
    email: z.string().min(1, 'Email é obrigatório').email('Email inválido'),
    password: z
      .string()
      .min(8, 'A senha deve ter no mínimo 8 caracteres')
      .regex(/[A-Za-z]/, 'A senha deve conter ao menos uma letra')
      .regex(/[0-9]/, 'A senha deve conter ao menos um número'),
    confirmPassword: z.string().min(1, 'Confirme sua senha'),
  })
  .refine((data) => data.password === data.confirmPassword, {
    message: 'As senhas não coincidem',
    path: ['confirmPassword'],
  })

export type RegisterFormValues = z.infer<typeof registerSchema>
