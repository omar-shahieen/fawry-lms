import { useEffect, useState } from 'react'

/** Server-clock countdown: re-derives seconds left from an absolute deadline every second. */
export function useCountdown(deadline: number | null): number {
  const [secondsLeft, setSecondsLeft] = useState(() =>
    deadline === null ? 0 : Math.max(0, Math.floor((deadline - Date.now()) / 1000)),
  )

  useEffect(() => {
    if (deadline === null) return
    const tick = () => setSecondsLeft(Math.max(0, Math.floor((deadline - Date.now()) / 1000)))
    tick()
    const interval = setInterval(tick, 1000)
    return () => clearInterval(interval)
  }, [deadline])

  return secondsLeft
}

export function formatCountdown(totalSeconds: number): string {
  const minutes = Math.floor(totalSeconds / 60)
  const seconds = totalSeconds % 60
  return `${minutes}:${String(seconds).padStart(2, '0')}`
}
