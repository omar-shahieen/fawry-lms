import { lazy, Suspense } from 'react'

/**
 * react-markdown + remark-gfm are the app's heaviest deps — lazy-loaded so
 * they land in their own chunk instead of inflating the main bundle.
 * Rendering stays markdown-only (never dangerouslySetInnerHTML).
 */
const MarkdownBody = lazy(async () => {
  const [{ default: ReactMarkdown }, { default: remarkGfm }] = await Promise.all([
    import('react-markdown'),
    import('remark-gfm'),
  ])
  function Body({ children }: { children: string }) {
    return <ReactMarkdown remarkPlugins={[remarkGfm]}>{children}</ReactMarkdown>
  }
  return { default: Body }
})

function MarkdownFallback() {
  return <div className="h-24 animate-pulse rounded-md bg-gray-100" aria-label="Loading content" />
}

export function Markdown({ children }: { children: string }) {
  return (
    <div className="prose prose-sm max-w-none prose-headings:text-gray-900 prose-p:text-gray-700 prose-a:text-indigo-600 prose-code:text-pink-600 prose-pre:bg-gray-900">
      <Suspense fallback={<MarkdownFallback />}>
        <MarkdownBody>{children}</MarkdownBody>
      </Suspense>
    </div>
  )
}
