import { Resend } from "resend";

const MAX_TEXT_LENGTH = 900;
const ALLOWED_TOPICS = new Set(["Idea", "Issue", "Delight"]);

export default {
  async fetch(request, env) {
    const url = new URL(request.url);

    if (request.method === "OPTIONS") {
      return jsonResponse({ ok: true });
    }

    if (url.pathname !== "/feedback") {
      return jsonResponse({ error: "Not found" }, 404);
    }

    if (request.method !== "POST") {
      return jsonResponse({ error: "Method not allowed" }, 405);
    }

    const contentType = request.headers.get("content-type") || "";
    if (!contentType.includes("application/json")) {
      return jsonResponse({ error: "Expected JSON body" }, 415);
    }

    const payload = await request.json().catch(() => null);
    const validationError = validatePayload(payload);
    if (validationError) {
      return jsonResponse({ error: validationError }, 400);
    }

    if (!env.RESEND_API_KEY || !env.FEEDBACK_TO_EMAIL || !env.FEEDBACK_FROM_EMAIL) {
      return jsonResponse({ error: "Feedback service is not configured" }, 500);
    }

    const resend = new Resend(env.RESEND_API_KEY);
    const { error } = await resend.emails.send({
      from: env.FEEDBACK_FROM_EMAIL,
      to: [env.FEEDBACK_TO_EMAIL],
      replyTo: payload.email || undefined,
      subject: `WaterMe feedback: ${payload.topic}`,
      text: formatFeedbackEmail(payload),
      html: formatFeedbackHtml(payload),
    });

    if (error) {
      return jsonResponse({ error: "Feedback could not be delivered" }, 502);
    }

    return jsonResponse({ ok: true }, 202);
  },
};

function validatePayload(payload) {
  if (!payload || typeof payload !== "object") return "Invalid feedback payload";
  if (!ALLOWED_TOPICS.has(payload.topic)) return "Choose a valid feedback topic";
  if (!isOptionalText(payload.name, 80)) return "Name is too long";
  if (!isOptionalText(payload.email, 120)) return "Email is too long";
  if (payload.email && !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(payload.email)) return "Email is invalid";
  if (!isRequiredText(payload.message, MAX_TEXT_LENGTH)) return "Feedback message is required";
  if (!isOptionalText(payload.appVersion, 40)) return "App version is invalid";
  if (!Number.isInteger(payload.androidSdk) || payload.androidSdk < 26 || payload.androidSdk > 200) {
    return "Android version is invalid";
  }
  return null;
}

function isRequiredText(value, maxLength) {
  return typeof value === "string" && value.trim().length > 0 && value.length <= maxLength;
}

function isOptionalText(value, maxLength) {
  return value === undefined || value === null || (typeof value === "string" && value.length <= maxLength);
}

function formatFeedbackEmail(payload) {
  const lines = [
    `Topic: ${payload.topic}`,
    `App version: ${payload.appVersion || "Unknown"}`,
    `Android SDK: ${payload.androidSdk}`,
  ];

  if (payload.name) lines.push(`Name: ${payload.name}`);
  if (payload.email) lines.push(`Email: ${payload.email}`);

  lines.push("", payload.message.trim());
  return lines.join("\n");
}

function formatFeedbackHtml(payload) {
  return `
    <h2>WaterMe feedback</h2>
    <p><strong>Topic:</strong> ${escapeHtml(payload.topic)}</p>
    <p><strong>App version:</strong> ${escapeHtml(payload.appVersion || "Unknown")}</p>
    <p><strong>Android SDK:</strong> ${escapeHtml(String(payload.androidSdk))}</p>
    ${payload.name ? `<p><strong>Name:</strong> ${escapeHtml(payload.name)}</p>` : ""}
    ${payload.email ? `<p><strong>Email:</strong> ${escapeHtml(payload.email)}</p>` : ""}
    <hr>
    <p>${escapeHtml(payload.message.trim()).replace(/\n/g, "<br>")}</p>
  `;
}

function escapeHtml(value) {
  return value
    .replace(/&/g, "&amp;")
    .replace(/</g, "&lt;")
    .replace(/>/g, "&gt;")
    .replace(/"/g, "&quot;")
    .replace(/'/g, "&#039;");
}

function jsonResponse(body, status = 200) {
  return new Response(JSON.stringify(body), {
    status,
    headers: {
      "Content-Type": "application/json",
    },
  });
}
