import type { ButtonHTMLAttributes, ReactNode } from 'react'

type Variant = 'primary' | 'secondary' | 'danger' | 'ghost'
type Size = 'sm' | 'md'

interface ButtonProps extends ButtonHTMLAttributes<HTMLButtonElement> {
  variant?: Variant
  size?: Size
  loading?: boolean
  children: ReactNode
}

const VARIANT_CLASSES: Record<Variant, string> = {
  primary:
    'bg-indigo-600 text-white hover:bg-indigo-500 focus-visible:outline-indigo-600 disabled:bg-indigo-300',
  secondary:
    'bg-white text-gray-700 ring-1 ring-gray-300 hover:bg-gray-50 focus-visible:outline-gray-400 disabled:text-gray-400',
  danger:
    'bg-red-600 text-white hover:bg-red-500 focus-visible:outline-red-600 disabled:bg-red-300',
  ghost: 'text-gray-700 hover:bg-gray-100 focus-visible:outline-gray-400 disabled:text-gray-400',
}

const SIZE_CLASSES: Record<Size, string> = {
  sm: 'px-2.5 py-1.5 text-xs',
  md: 'px-3.5 py-2 text-sm',
}

export function Button({
  variant = 'primary',
  size = 'md',
  loading = false,
  disabled,
  className = '',
  children,
  type = 'button',
  ...rest
}: ButtonProps) {
  return (
    <button
      type={type}
      disabled={disabled || loading}
      className={`inline-flex items-center justify-center gap-2 rounded-md font-medium shadow-xs transition-colors focus-visible:outline-2 focus-visible:outline-offset-2 disabled:cursor-not-allowed ${VARIANT_CLASSES[variant]} ${SIZE_CLASSES[size]} ${className}`}
      {...rest}
    >
      {loading && (
        <span className="size-3.5 animate-spin rounded-full border-2 border-current border-t-transparent" />
      )}
      {children}
    </button>
  )
}
