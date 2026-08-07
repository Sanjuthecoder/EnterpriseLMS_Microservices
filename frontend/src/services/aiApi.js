/**
 * aiApi.js — Dedicated Axios instance for the lms-ai-service.
 *
 * All AI endpoints go through /api/v1/ai/.
 * The gateway checks X-Subscription-Tier (forwarded from JWT claim).
 * The AI service's PremiumTierFilter will return 403 with upgradeRequired: true
 * if the user is not PREMIUM — callers should handle this specifically.
 *
 * Usage:
 *   import aiApi from '../services/aiApi';
 *
 *   // Generate pre-quiz
 *   const { data } = await aiApi.getPreQuiz(courseId, employeeId, companyId);
 *
 *   // Submit pre-quiz
 *   const { data } = await aiApi.submitPreQuiz(courseId, { sessionId, courseId, employeeId, answers });
 *
 *   // Get post-quiz
 *   const { data } = await aiApi.getPostQuiz(courseId, employeeId, companyId);
 *
 *   // Submit post-quiz
 *   const { data } = await aiApi.submitPostQuiz(courseId, { ...payload, currentProgressPercentage });
 *
 *   // Company Admin: get insights per course
 *   const { data } = await aiApi.getCompanyInsights(courseId, companyId);
 *
 *   // Super Admin: get platform insights per course
 *   const { data } = await aiApi.getPlatformInsights(courseId);
 *
 *   // Get lesson-level insight
 *   const { data } = await aiApi.getLessonInsight(lessonId, companyId, 'COMPANY');
 */
import axios from 'axios';

const aiAxios = axios.create({
  baseURL: import.meta.env.VITE_API_URL || 'http://localhost:8080',
  headers: { 'Content-Type': 'application/json' },
});

// Attach JWT token on every request
aiAxios.interceptors.request.use((config) => {
  const token = localStorage.getItem('token');
  if (token && token.includes('.')) {
    config.headers['Authorization'] = `Bearer ${token}`;
  }
  return config;
});



// ─── Insights ────────────────────────────────────────────────────────────────

/**
 * Company Admin: AI insights for a course within their company.
 */
const getCompanyInsights = (courseId, companyId) =>
  aiAxios.get(`/api/v1/ai/insights/company/courses/${courseId}`, {
    headers: { 'X-Company-Id': String(companyId) },
  });

/**
 * Super Admin: Platform-wide insights for a course (across all companies).
 */
const getPlatformInsights = (courseId) =>
  aiAxios.get(`/api/v1/ai/insights/platform/courses/${courseId}`);

/**
 * Generate/retrieve AI insight for a specific lesson.
 * @param {string} scope - 'COMPANY' or 'PLATFORM'
 */
const getLessonInsight = (lessonId, companyId, scope = 'COMPANY') =>
  aiAxios.get(`/api/v1/ai/insights/lessons/${lessonId}`, {
    params: { companyId, insightScope: scope },
  });

// ─── Error helpers ────────────────────────────────────────────────────────────

/**
 * Returns true if the error is a premium gate rejection (403 + upgradeRequired flag).
 * Use this to show the upgrade prompt toast instead of a generic error.
 */
export const isPremiumGateError = (error) =>
  error?.response?.status === 403 && error?.response?.data?.upgradeRequired === true;

/**
 * Extracts a user-facing message from an AI service error.
 * Falls back to a generic message for unexpected errors.
 */
export const getAiErrorMessage = (error) => {
  if (isPremiumGateError(error)) {
    return error.response.data.message;
  }
  return error?.response?.data?.message
    || error?.message
    || 'An unexpected error occurred. Please try again.';
};

const aiApi = {
  getCompanyInsights,
  getPlatformInsights,
  getLessonInsight,
};

export default aiApi;
