/**
 * §10 full UI walkthrough (docs/frontend-build.md).
 * Requires: backend http://localhost:8080, Vite http://localhost:5173, system Chrome.
 * Run: node scripts/walkthrough.mjs
 */
import { chromium } from 'playwright-core'

const BASE = 'http://localhost:5173'
const CHROME = 'C:\\Program Files\\Google\\Chrome\\Application\\chrome.exe'

const stamp = Date.now()
const ADMIN = { email: 'admin@lms.com', password: 'Admin123!' }
const studentEmail = `walk-student-${stamp}@example.com`
const studentPassword = 'Student123!'
const studentName = `Walk Student ${stamp}`
const observerEmail = `walk-observer-${stamp}@example.com`
const instructorEmail = `walk-instructor-${stamp}@example.com`
const instructorPassword = 'Instructor123!'
const instructorName = `Walk Instructor ${stamp}`
const courseTitle = `Walkthrough Course ${stamp}`
const courseCode = `W${String(stamp).slice(-6)}`
const courseTerm = 'Fall 2026'
const sectionTitle = 'Week 1 — Intro'
const contentTitle = 'Lecture notes'
const contentBody = '# Hello **walkthrough**\n\n- point one\n- point two\n'
const quizTitle = 'Walkthrough Quiz'
const quizDuration = '15'
const announcementTitle = 'Welcome to Walkthrough'
const announcementBody = 'Please read Week 1 before Friday.'
const discussionPostTitle = 'Question about Week 1'
const discussionPostBody = 'When is office hours?'
const discussionReplyBody = 'Office hours are Thursdays at 3pm.'

/** @type {Array<{ step: string, ok: boolean, detail?: string }>} */
const results = []
let courseId = ''
let courseHref = ''

function pass(step, detail) {
  results.push({ step, ok: true, detail })
  console.log(`PASS  ${step}${detail ? ` — ${detail}` : ''}`)
}

function fail(step, detail) {
  results.push({ step, ok: false, detail })
  console.log(`FAIL  ${step}${detail ? ` — ${detail}` : ''}`)
}

async function assert(cond, step, detail) {
  if (cond) {
    pass(step, detail)
    return true
  }
  fail(step, detail)
  return false
}

async function login(page, email, password) {
  await page.goto(`${BASE}/login`)
  await page.getByLabel('Email').fill(email)
  await page.getByLabel('Password').fill(password)
  await page.getByRole('button', { name: 'Sign in' }).click()
  await page.waitForURL(/\/dashboard/, { timeout: 15000 })
}

async function logout(page) {
  await page.getByRole('button', { name: 'Logout' }).click()
  await page.waitForURL(/\/login/, { timeout: 15000 })
}

async function fillByLabel(page, label, value) {
  await page.getByLabel(label, { exact: true }).fill(value)
}

function parseTimer(text) {
  const m = /(\d+):(\d{2})/.exec(text)
  if (!m) return null
  return Number(m[1]) * 60 + Number(m[2])
}

/** Logs failing HTTP responses + browser console errors so failures are diagnosable. */
function wire(p, tag) {
  p.on('response', (res) => {
    if (res.status() >= 400) console.log(`  [${tag}] HTTP ${res.status()} ${res.url()}`)
  })
  p.on('console', (msg) => {
    if (msg.type() === 'error') console.log(`  [${tag}] console.error: ${msg.text()}`)
  })
  p.on('pageerror', (err) => console.log(`  [${tag}] pageerror: ${err.message}`))
}

/**
 * Walk a paginated list (URL-synced page param) until `cell` is visible.
 * Seed data grows with every walkthrough run, so a plain first-page wait goes stale.
 */
async function findByPagination(p, url, cell, maxPages = 25) {
  await p.goto(url)
  for (let attempt = 0; attempt < maxPages; attempt++) {
    try {
      await cell.waitFor({ timeout: 3000 })
      return true
    } catch {
      const next = p.getByRole('button', { name: 'Next' })
      if ((await next.count()) === 0 || !(await next.isEnabled())) return false
      await next.click()
    }
  }
  return false
}

/**
 * The users list exposes only a role filter + pagination (no search) — filter by role
 * and walk pages until the account shows up.
 */
function findUserInList(p, email, role) {
  return findByPagination(p, `${BASE}/admin/users?role=${role}`, p.getByText(email).first())
}

async function run() {
  const browser = await chromium.launch({ executablePath: CHROME, headless: true })
  const context = await browser.newContext({ viewport: { width: 1280, height: 800 } })
  const page = await context.newPage()
  page.setDefaultTimeout(20000)
  wire(page, 'main')

  try {
    // ── 1. Sign up → Student dashboard ──────────────────────────────────
    await page.goto(`${BASE}/signup`)
    await fillByLabel(page, 'Full name', studentName)
    await fillByLabel(page, 'Email', studentEmail)
    await fillByLabel(page, 'Password', studentPassword)
    await page.getByRole('button', { name: 'Sign up' }).click()
    await page.waitForURL(/\/dashboard/, { timeout: 15000 })
    const roleText1 = await page.locator('aside').innerText()
    await assert(
      roleText1.includes('STUDENT'),
      '1. Signup lands on Student dashboard',
      roleText1.includes('STUDENT') ? 'role STUDENT in sidebar' : roleText1.slice(0, 120),
    )
    await page.getByText('Your enrolled courses and quiz status.').waitFor({ timeout: 15000 })
    await assert(true, '1b. Student dashboard regions render')

    // ── 2. Admin → instructor, course, assign ───────────────────────────
    await logout(page)
    await login(page, ADMIN.email, ADMIN.password)

    await page.goto(`${BASE}/admin/users`)
    await page.getByRole('button', { name: 'New user' }).click()
    await page.getByRole('dialog', { name: 'Create user' }).getByLabel('Full name').fill(instructorName)
    await page.getByRole('dialog', { name: 'Create user' }).getByLabel('Email').fill(instructorEmail)
    await page.getByRole('dialog', { name: 'Create user' }).getByLabel('Password').fill(instructorPassword)
    await page.getByRole('dialog', { name: 'Create user' }).getByLabel('Role').selectOption('INSTRUCTOR')
    await page.getByRole('dialog', { name: 'Create user' }).getByRole('button', { name: 'Create user' }).click()
    await page.getByRole('dialog', { name: 'Create user' }).waitFor({ state: 'detached', timeout: 15000 })
    const instructorListed = await findUserInList(page, instructorEmail, 'INSTRUCTOR')
    await assert(
      instructorListed,
      '2a. Admin creates Instructor via /admin/users',
      instructorListed ? undefined : 'instructor not found in the users list',
    )

    await page.goto(`${BASE}/admin/courses`)
    await page.getByRole('button', { name: 'New course' }).click()
    const createDialog = page.getByRole('dialog', { name: 'Create course' })
    await createDialog.locator('label:has-text("Title") input').fill(courseTitle)
    await createDialog.locator('label:has-text("Description") textarea').fill('Created by the §10 walkthrough.')
    await createDialog.locator('label:has-text("Code") input').fill(courseCode)
    await createDialog.locator('label:has-text("Term") input').fill(courseTerm)
    await createDialog.getByLabel('Instructor').selectOption({ label: `${instructorName} (${instructorEmail})` })
    await createDialog.getByRole('button', { name: 'Create course' }).click()
    await createDialog.waitFor({ state: 'detached', timeout: 15000 })
    const courseLink = page.getByRole('link', { name: courseTitle }).first()
    const courseListed = await findByPagination(page, `${BASE}/admin/courses`, courseLink)
    courseHref = courseListed ? (await courseLink.getAttribute('href')) || '' : ''
    courseId = courseHref.split('/').filter(Boolean).pop() || ''
    await assert(
      Boolean(courseId) && /^\d+$/.test(courseId),
      '2b. Admin creates Course',
      courseListed ? `id=${courseId}` : 'course not found in the admin list',
    )

    const instructorRow = page.getByRole('row').filter({ hasText: courseCode })
    const instructorCell = await instructorRow.innerText()
    await assert(
      instructorCell.includes(instructorName),
      '2c. Course shows assigned Instructor in admin table',
      instructorCell.replace(/\s+/g, ' ').slice(0, 160),
    )

    // ── 3. Student catalog → enroll ─────────────────────────────────────
    await logout(page)
    await login(page, studentEmail, studentPassword)
    await page.goto(`${BASE}/courses`)
    await page.getByPlaceholder('Search by title…').fill(courseTitle)
    await page.getByRole('button', { name: 'Apply' }).click()
    await page.getByRole('link', { name: courseTitle }).first().waitFor({ timeout: 15000 })
    await page.getByRole('link', { name: courseTitle }).first().click()
    await page.waitForURL(new RegExp(`/courses/${courseId}$`), { timeout: 15000 })
    await page.getByRole('button', { name: 'Enroll', exact: true }).click()
    await page.getByText('You are enrolled in this course.').waitFor({ timeout: 15000 })
    await assert(true, '3. Student enrolls from catalog')

    // ── 4. Instructor: section, content, quiz, questions, publish, announcement ──
    await logout(page)
    await login(page, instructorEmail, instructorPassword)
    await page.goto(`${BASE}/courses/${courseId}`)
    await page.goto(`${BASE}/courses/${courseId}/content/manage`)
    await page.getByLabel('New section title').waitFor({ timeout: 15000 })
    await fillByLabel(page, 'New section title', sectionTitle)
    await page.getByRole('button', { name: 'Add section' }).click()
    await page.getByRole('button', { name: new RegExp(`\\d+\\. ${sectionTitle}`) }).waitFor({ timeout: 15000 })
    await page.getByRole('button', { name: new RegExp(`\\d+\\. ${sectionTitle}`) }).click()
    await page.getByRole('button', { name: 'Add content' }).click()
    await fillByLabel(page, 'Title', contentTitle)
    await fillByLabel(page, 'Markdown body', contentBody)
    await page.getByRole('button', { name: 'Add content' }).click()
    await page.getByText(contentTitle).first().waitFor({ timeout: 15000 })
    await assert(true, '4a. Instructor creates Section + Markdown content')

    await page.goto(`${BASE}/courses/${courseId}/quizzes/manage`)
    await page.getByRole('button', { name: 'New quiz' }).click()
    const quizDialog = page.getByRole('dialog', { name: 'Create quiz' })
    await quizDialog.getByLabel('Title').fill(quizTitle)
    await quizDialog.getByLabel('Duration (minutes)').fill(quizDuration)
    await quizDialog.getByRole('button', { name: 'Create quiz' }).click()
    await quizDialog.waitFor({ state: 'detached', timeout: 15000 })
    await page.getByText(quizTitle).first().waitFor({ timeout: 15000 })
    await page.getByRole('link', { name: 'Edit' }).first().click()
    await page.waitForURL(/\/quizzes\/\d+\/edit/, { timeout: 15000 })
    const editParts = page.url().split('/').filter(Boolean)
    const quizId = editParts[editParts.length - 2]
    await assert(Boolean(quizId) && /^\d+$/.test(quizId), '4b. Instructor creates Quiz', `id=${quizId}`)

    // Header opener and the editor form's submit button share the "Add question" label.
    const openQuestionForm = () => page.getByRole('button', { name: 'Add question', exact: true }).first().click()
    const submitQuestionForm = () =>
      page
        .locator('form')
        .filter({ hasText: 'exactly one correct answer' })
        .getByRole('button', { name: 'Add question', exact: true })
        .click()
    // A create only finished once the editor unmounts AND the question shows in the list.
    // (getByText is unsafe here: a React textarea's textContent mirrors its value.)
    const questionSaved = async (text) => {
      await page
        .locator('form')
        .filter({ hasText: 'exactly one correct answer' })
        .waitFor({ state: 'detached', timeout: 15000 })
      await page.locator('p').filter({ hasText: text }).first().waitFor({ timeout: 15000 })
    }

    // Question 1 — multi-correct attempt (server rejects ≠1 correct), then fix
    await openQuestionForm()
    await fillByLabel(page, 'Question', 'What is 2 + 2?')
    await page.getByPlaceholder('Option 1').fill('3')
    await page.getByPlaceholder('Option 2').fill('4')
    await page.getByRole('button', { name: 'Add option' }).click()
    await page.getByPlaceholder('Option 3').fill('5')
    await page.getByLabel('Mark option 1 correct').check()
    await page.getByLabel('Mark option 2 correct').check()
    await submitQuestionForm()
    const multiCorrectErr = page.getByRole('alert').filter({ hasText: /exactly one correct/i })
    await multiCorrectErr.waitFor({ timeout: 15000 })
    const multiMsg = await multiCorrectErr.innerText()
    await assert(
      /exactly one correct/i.test(multiMsg),
      '4c. Multi-correct question rejected with 400 copy',
      multiMsg.slice(0, 120),
    )
    await page.getByLabel('Mark option 1 correct').uncheck()
    await submitQuestionForm()
    await questionSaved('What is 2 + 2?')

    // Question 2
    await openQuestionForm()
    await fillByLabel(page, 'Question', 'Capital of France?')
    await page.getByPlaceholder('Option 1').fill('Berlin')
    await page.getByPlaceholder('Option 2').fill('Paris')
    await page.getByLabel('Mark option 2 correct').check()
    await submitQuestionForm()
    await questionSaved('Capital of France?')

    // Question 3
    await openQuestionForm()
    await fillByLabel(page, 'Question', 'Primary colors include?')
    await page.getByPlaceholder('Option 1').fill('Red')
    await page.getByPlaceholder('Option 2').fill('Green')
    await page.getByLabel('Mark option 1 correct').check()
    await submitQuestionForm()
    await questionSaved('Primary colors include?')
    await assert(true, '4d. Three questions built')

    await page.getByRole('button', { name: 'Publish', exact: true }).click()
    await page.getByText('Published').first().waitFor({ timeout: 15000 })
    await assert(true, '4e. Quiz published')

    await page.goto(`${BASE}/courses/${courseId}/announcements`)
    await page.getByRole('button', { name: 'New announcement' }).first().click()
    const annDialog = page.getByRole('dialog', { name: 'New announcement' })
    await annDialog.getByLabel('Title').fill(announcementTitle)
    await annDialog.getByLabel('Body').fill(announcementBody)
    await annDialog.getByRole('button', { name: 'Publish' }).click()
    await annDialog.waitFor({ state: 'detached', timeout: 15000 })
    await page.getByText(announcementTitle).first().waitFor({ timeout: 15000 })
    await assert(true, '4f. Instructor creates Announcement')

    // ── 5. Student quiz take flow ───────────────────────────────────────
    await logout(page)
    await login(page, studentEmail, studentPassword)
    await page.goto(`${BASE}/courses/${courseId}/content`)
    await page.getByRole('button', { name: new RegExp(sectionTitle) }).click()
    await page.getByRole('button', { name: contentTitle }).click()
    const rendered = page.locator('h1:has-text("Hello")')
    await rendered.waitFor({ timeout: 15000 })
    await assert(true, '5a. Student reads Markdown content (rendered, not raw HTML)')

    await page.goto(`${BASE}/courses/${courseId}/quizzes`)
    await page.getByRole('link', { name: 'Open' }).first().click()
    await page.waitForURL(new RegExp(`/quizzes/${quizId}`), { timeout: 15000 })
    const startBtn = page.getByRole('button', { name: 'Start quiz' })
    await startBtn.waitFor({ timeout: 15000 })
    const landingVisible = await page.getByText('Starting creates your single attempt and starts the timer.').isVisible()
    await assert(landingVisible, '5b. Landing card before Start (no side-effect GET yet)')

    // Second tab opened after Start so resume uses attempts/me
    await startBtn.click()
    const timer = page.locator('[role="timer"]')
    await timer.waitFor({ timeout: 20000 })
    const t1 = parseTimer(await timer.innerText())
    await assert(t1 !== null && t1 > 0 && t1 <= Number(quizDuration) * 60, '5c. Timer visible after Start', `t1=${t1}s`)

    await page.waitForTimeout(2500)
    const t2pre = parseTimer(await timer.innerText())
    await page.reload()
    await timer.waitFor({ timeout: 20000 })
    const t3 = parseTimer(await timer.innerText())
    const timerOk =
      t3 !== null &&
      t2pre !== null &&
      t3 <= t2pre + 1 &&
      t3 >= t2pre - 5 &&
      t3 < Number(quizDuration) * 60
    await assert(timerOk, '5d. Reload does not reset countdown', `pre-reload=${t2pre}s post-reload=${t3}s`)

    // Second tab for the 409 double-submit test. sessionStorage is per-tab, so a fresh
    // tab has no session — log in again there (which also exercises page1's 401→refresh
    // recovery, since the server keeps a single active access token per user).
    const page2 = await context.newPage()
    page2.setDefaultTimeout(20000)
    wire(page2, 'tab2')
    await login(page2, studentEmail, studentPassword)
    await page2.goto(`${BASE}/quizzes/${quizId}`)
    await page2.locator('[role="timer"]').waitFor({ timeout: 20000 })

    // Select first radio per question group (names are server question ids)
    const radioNames = await page.locator('input[type="radio"]').evaluateAll((els) => [
      ...new Set(els.map((el) => el.getAttribute('name') || '')),
    ])
    for (const name of radioNames) {
      if (name) await page.locator(`input[type="radio"][name="${name}"]`).first().check()
    }
    const radioCount = await page.locator('input[type="radio"]').count()
    await assert(radioCount >= 6 && radioNames.length === 3, '5e. Questions render with options', `radios=${radioCount} groups=${radioNames.length}`)

    await page.getByRole('button', { name: /Submit (quiz|now)/ }).click()
    await page.getByRole('heading', { name: 'Review' }).waitFor({ timeout: 20000 })
    const scoreBadge = page.locator('text=/Score:\\s*\\d+/').first()
    await scoreBadge.waitFor({ timeout: 15000 })
    const scoreText = await scoreBadge.innerText()
    await assert(/Score:\s*\d+/.test(scoreText), '5f. Submit → score shown in review', scoreText)

    // Second submit from the other tab → 409 copy
    const p2groups = await page2.locator('input[type="radio"]').evaluateAll((els) => [
      ...new Set(els.map((el) => el.getAttribute('name') || '')),
    ])
    if (p2groups.length > 0) {
      await page2.locator(`input[type="radio"][name="${p2groups[0]}"]`).first().check()
      await page2.getByRole('button', { name: /Submit (quiz|now)/ }).click()
      const conflict = page2.getByRole('alert').filter({ hasText: /already submitted/i })
      await conflict.waitFor({ timeout: 15000 })
      const conflictMsg = await conflict.innerText()
      await assert(
        /already submitted/i.test(conflictMsg),
        '5g. Second submit → 409 copy',
        conflictMsg.slice(0, 140),
      )
      await page2.getByRole('button', { name: 'View review' }).click()
      await page2.getByRole('heading', { name: 'Review' }).waitFor({ timeout: 15000 })
      await assert(true, '5h. 409 → View review lands on review')
    } else {
      const alreadyReview = await page2
        .getByRole('heading', { name: 'Review' })
        .isVisible()
        .catch(() => false)
      if (alreadyReview) {
        pass('5g. Second tab resumes into review (attempt already submitted)', 'no runner UI')
      } else {
        fail('5g. Second submit → 409 copy', 'page2 had no radios and no review')
      }
    }
    await page2.close()

    // ── 6. Student grades + discussion post ─────────────────────────────
    await page.goto(`${BASE}/grades`)
    await page.getByRole('heading', { name: 'Grades' }).waitFor({ timeout: 15000 })
    await page.getByText(courseTitle).first().waitFor({ timeout: 15000 })
    await page.getByText(quizTitle).first().waitFor({ timeout: 15000 })
    await assert(true, '6a. /grades shows grouped results')

    await page.goto(`${BASE}/courses/${courseId}/discussion`)
    await fillByLabel(page, 'Title', discussionPostTitle)
    await fillByLabel(page, 'Body', discussionPostBody)
    await page.getByRole('button', { name: 'Post', exact: true }).click()
    await page.getByText(discussionPostTitle).first().waitFor({ timeout: 15000 })
    await assert(true, '6b. Student creates Discussion post')

    // ── 7. Instructor reply; no reply-to-reply ──────────────────────────
    await logout(page)
    await login(page, instructorEmail, instructorPassword)
    await page.goto(`${BASE}/courses/${courseId}/discussion`)
    await page.getByText(discussionPostTitle).first().waitFor({ timeout: 15000 })
    await page.getByRole('button', { name: 'Reply', exact: true }).first().click()
    await fillByLabel(page, 'Your reply', discussionReplyBody)
    await page.getByRole('button', { name: 'Reply', exact: true }).last().click()
    await page.locator('p').filter({ hasText: discussionReplyBody }).first().waitFor({ timeout: 15000 })
    await assert(true, '7a. Instructor replies to post')

    // ReplyItem has Edit/Delete only — exactly one post-level Reply control remains
    const replyBtnCount = await page.getByRole('button', { name: 'Reply', exact: true }).count()
    await assert(
      replyBtnCount === 1,
      '7b. Reply rendered; only post-level Reply remains (no reply-to-reply, §7.8.3)',
      `reply-buttons=${replyBtnCount}`,
    )

    // ── 8. Student reads announcement + dashboard ───────────────────────
    await logout(page)
    await login(page, studentEmail, studentPassword)
    await page.goto(`${BASE}/courses/${courseId}/announcements`)
    await page.getByText(announcementTitle).first().waitFor({ timeout: 15000 })
    await page.getByText(announcementBody).first().waitFor({ timeout: 15000 })
    await assert(true, '8a. Student reads announcement')

    await page.goto(`${BASE}/dashboard`)
    await page.getByText(courseTitle).first().waitFor({ timeout: 15000 })
    await page.getByText(quizTitle).first().waitFor({ timeout: 15000 })
    const attempted = await page.getByText('Attempted').first().isVisible()
    await assert(attempted, '8b. Student dashboard shows course + Attempted badge')

    // ── 9. Instructor dashboard ─────────────────────────────────────────
    await logout(page)
    await login(page, instructorEmail, instructorPassword)
    await page.goto(`${BASE}/dashboard`)
    await page.getByRole('heading', { name: 'Your courses', exact: true }).waitFor({ timeout: 15000 })
    await page.getByText(courseTitle).first().waitFor({ timeout: 15000 })
    await page.getByRole('heading', { name: 'Your announcements', exact: true }).waitFor({ timeout: 15000 })
    await page.getByText(announcementTitle).first().waitFor({ timeout: 15000 })
    const hasAvg = await page.getByText(/avg|submitted attempt/i).first().isVisible()
    await assert(hasAvg, '9. Instructor dashboard: courses, quiz summary, announcements')

    // ── 10. Admin dashboard + admin screens ─────────────────────────────
    await logout(page)
    await login(page, ADMIN.email, ADMIN.password)
    await page.goto(`${BASE}/dashboard`)
    await page.getByText('Overall system counts.').waitFor({ timeout: 15000 })
    await page.getByText('Courses', { exact: true }).first().waitFor({ timeout: 15000 })
    await page.getByText('Enrollments', { exact: true }).first().waitFor({ timeout: 15000 })
    await assert(true, '10a. Admin dashboard shows counts')

    await page.goto(`${BASE}/admin/users`)
    await page.getByRole('button', { name: 'New user' }).waitFor({ timeout: 15000 })
    await assert(await findUserInList(page, instructorEmail, 'INSTRUCTOR'), '10b. /admin/users works')

    await page.goto(`${BASE}/admin/courses`)
    await page.getByRole('button', { name: 'New course' }).waitFor({ timeout: 15000 })
    await assert(
      await findByPagination(page, `${BASE}/admin/courses`, page.getByText(courseTitle).first()),
      '10c. /admin/courses works',
    )

    // ── 11. Negative pass ───────────────────────────────────────────────
    // 11a. Student → /admin/users → 403 page
    await logout(page)
    await login(page, studentEmail, studentPassword)
    await page.goto(`${BASE}/admin/users`)
    await page.getByText('403').waitFor({ timeout: 15000 })
    await page.getByText(/don’t have access to this page|don't have access to this page/).waitFor({ timeout: 15000 })
    await assert(true, '11a. Student /admin/users → 403 page')

    // 11b. Unenrolled student → course content → not enrolled (no crash)
    await logout(page)
    await page.goto(`${BASE}/signup`)
    await fillByLabel(page, 'Full name', `Walk Observer ${stamp}`)
    await fillByLabel(page, 'Email', observerEmail)
    await fillByLabel(page, 'Password', studentPassword)
    await page.getByRole('button', { name: 'Sign up' }).click()
    await page.waitForURL(/\/dashboard/, { timeout: 15000 })
    await page.goto(`${BASE}/courses/${courseId}/content`)
    await page.getByText('You are not enrolled in this course.').waitFor({ timeout: 15000 })
    await assert(true, '11b. Unenrolled content → "not enrolled" state, not a crash')

    // 11c. Logout → back button cannot resume session
    const refreshBefore = await page.evaluate(() => sessionStorage.getItem('lms.refreshToken'))
    await page.getByRole('button', { name: 'Logout' }).click()
    await page.waitForURL(/\/login/, { timeout: 15000 })
    const sessionKeys = await page.evaluate(() => Object.keys(sessionStorage))
    const refreshAfter = await page.evaluate(() => sessionStorage.getItem('lms.refreshToken'))
    await page.goBack()
    await page.waitForTimeout(1000)
    if (!page.url().includes('/login')) await page.reload()
    await page.waitForTimeout(2000)
    const finalUrl = page.url()
    const onLogin =
      finalUrl.includes('/login') ||
      (await page.getByRole('heading', { name: 'Sign in' }).isVisible().catch(() => false))
    await assert(
      onLogin && !refreshAfter && refreshBefore !== null,
      '11c. Logout clears refresh token; back button cannot resume session',
      `keys-after=[${sessionKeys.join(',')}] refreshBefore=${Boolean(refreshBefore)} refreshAfter=${refreshAfter} final=${finalUrl}`,
    )
  } catch (err) {
    fail('UNCAUGHT', err instanceof Error ? `${err.message}` : String(err))
    console.log(`  url at failure: ${page.url()}`)
    try {
      const shot = `walkthrough-failure-${Date.now()}.png`
      await page.screenshot({ path: shot, fullPage: true })
      console.error(`Screenshot: ${shot}`)
    } catch {
      /* ignore */
    }
  } finally {
    await browser.close()
  }

  const passed = results.filter((r) => r.ok).length
  const failed = results.filter((r) => !r.ok)
  console.log('\n── §10 walkthrough summary ──')
  console.log(`${passed}/${results.length} passed`)
  if (failed.length) {
    for (const f of failed) console.log(`  FAIL ${f.step}: ${f.detail ?? ''}`)
    process.exitCode = 1
  } else {
    console.log('All steps passed.')
  }
}

run().catch((err) => {
  console.error(err)
  process.exit(1)
})
