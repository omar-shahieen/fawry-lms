import type { InputHTMLAttributes, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react'

interface FieldWrapperProps {
  label: string
  error?: string
  hint?: string
  htmlFor: string
  children: React.ReactNode
}

export function FieldWrapper({ label, error, hint, htmlFor, children }: FieldWrapperProps) {
  return (
    <div className="space-y-1.5">
      <label htmlFor={htmlFor} className="block text-sm font-medium text-gray-800">
        {label}
      </label>
      {children}
      {hint && !error && <p className="text-xs text-gray-500">{hint}</p>}
      {error && (
        <p className="text-xs font-medium text-red-600" role="alert">
          {error}
        </p>
      )}
    </div>
  )
}

const controlClasses =
  'block w-full rounded-md border-0 bg-white px-3 py-2 text-sm text-gray-900 shadow-sm ring-1 ring-inset ring-gray-300 placeholder:text-gray-400 focus:ring-2 focus:ring-inset focus:ring-indigo-600 disabled:bg-gray-50 disabled:text-gray-500'

interface InputProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string
  error?: string
  hint?: string
}

export function Input({ label, error, hint, id, className = '', ...rest }: InputProps) {
  const inputId = id ?? `input-${rest.name ?? label}`
  return (
    <FieldWrapper label={label} error={error} hint={hint} htmlFor={inputId}>
      <input
        id={inputId}
        aria-invalid={error ? true : undefined}
        className={`${controlClasses} ${error ? 'ring-red-400 focus:ring-red-500' : ''} ${className}`}
        {...rest}
      />
    </FieldWrapper>
  )
}

interface TextareaProps extends TextareaHTMLAttributes<HTMLTextAreaElement> {
  label: string
  error?: string
  hint?: string
}

export function Textarea({ label, error, hint, id, className = '', ...rest }: TextareaProps) {
  const inputId = id ?? `textarea-${rest.name ?? label}`
  return (
    <FieldWrapper label={label} error={error} hint={hint} htmlFor={inputId}>
      <textarea
        id={inputId}
        aria-invalid={error ? true : undefined}
        className={`${controlClasses} ${error ? 'ring-red-400 focus:ring-red-500' : ''} ${className}`}
        {...rest}
      />
    </FieldWrapper>
  )
}

interface SelectProps extends SelectHTMLAttributes<HTMLSelectElement> {
  label: string
  error?: string
}

export function Select({ label, error, id, className = '', children, ...rest }: SelectProps) {
  const inputId = id ?? `select-${rest.name ?? label}`
  return (
    <FieldWrapper label={label} error={error} htmlFor={inputId}>
      <select
        id={inputId}
        aria-invalid={error ? true : undefined}
        className={`${controlClasses} ${error ? 'ring-red-400 focus:ring-red-500' : ''} ${className}`}
        {...rest}
      >
        {children}
      </select>
    </FieldWrapper>
  )
}
