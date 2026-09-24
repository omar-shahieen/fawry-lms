interface SpinnerProps {
  label?: string
}

export function Spinner({ label }: SpinnerProps) {
  return (
    <div className="flex items-center gap-2 text-gray-500" role="status">
      <span className="size-5 animate-spin rounded-full border-2 border-gray-300 border-t-indigo-600" />
      {label && <span className="text-sm">{label}</span>}
    </div>
  )
}
