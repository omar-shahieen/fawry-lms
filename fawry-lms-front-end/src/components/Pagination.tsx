interface PaginationProps {
  page: number
  totalPages: number
  onPageChange: (page: number) => void
  /** Optional total element count for display. */
  totalElements?: number
}

export function Pagination({ page, totalPages, onPageChange, totalElements }: PaginationProps) {
  if (totalPages <= 1) return null

  return (
    <nav className="flex items-center justify-between gap-4 border-t border-gray-200 pt-4" aria-label="Pagination">
      <p className="text-sm text-gray-600">
        Page {page + 1} of {totalPages}
        {totalElements !== undefined && ` · ${totalElements} total`}
      </p>
      <div className="flex gap-2">
        <button
          type="button"
          disabled={page <= 0}
          onClick={() => onPageChange(page - 1)}
          className="rounded-md bg-white px-3 py-1.5 text-sm font-medium text-gray-700 ring-1 ring-gray-300 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40"
        >
          Previous
        </button>
        <button
          type="button"
          disabled={page >= totalPages - 1}
          onClick={() => onPageChange(page + 1)}
          className="rounded-md bg-white px-3 py-1.5 text-sm font-medium text-gray-700 ring-1 ring-gray-300 hover:bg-gray-50 disabled:cursor-not-allowed disabled:opacity-40"
        >
          Next
        </button>
      </div>
    </nav>
  )
}
