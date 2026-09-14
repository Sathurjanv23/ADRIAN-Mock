package com.nova.emergency.service;

import com.nova.emergency.model.Incident;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

/**
 * EmailService — Centralised email delivery for ADRIAN Emergency Response Network.
 *
 * All public methods are @Async so that email failures never block the main transaction.
 * Return values are void so callers fire-and-forget.
 * Every send is wrapped in try/catch — a failed email is logged but never propagates.
 */
@Service
public class EmailService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Value("${spring.mail.username:}")
    private String fromEmail;

    @Value("${nova.otp.expiry-seconds:300}")
    private int otpExpirySeconds;

    // ─── Helpers ─────────────────────────────────────────────────────────

    private String formatSenderAddress() {
        return fromEmail != null && !fromEmail.isBlank() ? fromEmail : "noreply@adrian-emergency.lk";
    }

    private boolean isEmailConfigured() {
        return fromEmail != null && !fromEmail.isBlank();
    }

    private String formatOtpExpiry() {
        if (otpExpirySeconds >= 60) {
            int minutes = otpExpirySeconds / 60;
            return minutes + " minute" + (minutes == 1 ? "" : "s");
        }
        return otpExpirySeconds + " seconds";
    }

    // ─── OTP / Verification Email ─────────────────────────────────────────

    /**
     * Sends a 6-digit OTP email for account verification or password reset.
     * Async — failure is logged only; never throws.
     */
    @Async
    public void sendOtpEmail(String toEmail, String otp, String name) {
        if (!isEmailConfigured()) {
            log.warn("Email not configured (SMTP_USERNAME empty) — OTP email skipped for: {}", toEmail);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(formatSenderAddress(), "ADRIAN — Emergency Response Network");
            helper.setTo(toEmail);
            helper.setSubject("ADRIAN — Verification Code: " + otp);

            String displayName = (name != null && !name.trim().isEmpty()) ? name : "Responder";
            String expiryText = formatOtpExpiry();

            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Verification Code</title>
                </head>
                <body style="margin:0;padding:0;background-color:#080d1a;font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#e2e8f0;">
                  <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color:#080d1a;padding:40px 20px;">
                    <tr>
                      <td align="center">
                        <table width="100%%" style="max-width:540px;background-color:#0f172a;border:1px solid #1e293b;border-radius:16px;overflow:hidden;box-shadow:0 10px 25px rgba(0,0,0,0.5);">
                          <!-- Header -->
                          <tr>
                            <td style="padding:32px 32px 20px 32px;text-align:center;background:linear-gradient(180deg, rgba(211,47,47,0.08) 0%%, rgba(15,23,42,0) 100%%);border-bottom:1px solid #1e293b;">
                              <div style="display:inline-block;padding:8px 16px;background:rgba(211,47,47,0.1);border:1px solid rgba(211,47,47,0.3);border-radius:30px;margin-bottom:12px;">
                                <span style="font-family:monospace;font-size:12px;font-weight:bold;color:#ef4444;letter-spacing:1.5px;">ADRIAN</span>
                              </div>
                              <h1 style="margin:0;font-size:22px;font-weight:800;color:#ffffff;letter-spacing:-0.5px;">Email Verification</h1>
                              <p style="margin:6px 0 0 0;font-size:13px;color:#94a3b8;">Autonomous Disaster Response &amp; Integrated Aid Network</p>
                            </td>
                          </tr>
                          <!-- Body -->
                          <tr>
                            <td style="padding:32px;">
                              <p style="margin:0 0 16px 0;font-size:15px;color:#cbd5e1;line-height:1.5;">
                                Hello <strong style="color:#ffffff;">%s</strong>,
                              </p>
                              <p style="margin:0 0 24px 0;font-size:14px;color:#94a3b8;line-height:1.6;">
                                Use the 6-digit code below to verify your ADRIAN account. The code is valid for <strong style="color:#f1f5f9;">%s</strong>.
                              </p>
                              <!-- OTP Box -->
                              <div style="background-color:#080d1a;border:2px dashed #ef4444;border-radius:12px;padding:24px;text-align:center;margin:24px 0;">
                                <div style="font-size:11px;font-family:monospace;color:#94a3b8;letter-spacing:1px;text-transform:uppercase;margin-bottom:8px;">Your One-Time Password</div>
                                <div style="font-family:'Courier New',Courier,monospace;font-size:36px;font-weight:900;letter-spacing:10px;color:#ef4444;">%s</div>
                                <div style="font-size:11px;color:#ef4444;margin-top:8px;font-weight:600;">⏱ Expires in %s</div>
                              </div>
                              <p style="margin:24px 0 0 0;font-size:12px;color:#64748b;line-height:1.5;">
                                If you did not request this code, you can safely ignore this email. Never share this code with anyone.
                              </p>
                            </td>
                          </tr>
                          <!-- Footer -->
                          <tr>
                            <td style="padding:20px 32px;background-color:#080d1a;border-top:1px solid #1e293b;text-align:center;">
                              <p style="margin:0;font-size:11px;color:#475569;">
                                © 2026 ADRIAN · Autonomous Disaster Response &amp; Integrated Aid Network · Sri Lanka
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(displayName, expiryText, otp, expiryText);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("✅ OTP email sent to: {}", toEmail);

        } catch (Exception e) {
            log.error("❌ Failed to send OTP email to {}: {}", toEmail, e.getMessage());
        }
    }

    // ─── Emergency Alert Email ─────────────────────────────────────────────

    /**
     * Sends an emergency alert email to a rescue team or hospital recipient.
     *
     * IMPORTANT: This method is @Async — it never blocks the calling thread.
     * A failure here must never cause incident creation to fail.
     *
     * @param toEmail        Recipient email address
     * @param recipientName  Recipient's display name
     * @param incident       The emergency incident
     * @param recipientRole  "rescue_team" or "hospital" — controls subject wording
     */
    @Async
    public void sendEmergencyAlertEmail(String toEmail, String recipientName,
                                         Incident incident, String recipientRole) {
        if (!isEmailConfigured()) {
            log.warn("Email not configured (SMTP_USERNAME empty) — emergency alert email skipped for: {}", toEmail);
            return;
        }
        if (toEmail == null || toEmail.isBlank()) {
            log.warn("Emergency alert email skipped — recipient email is blank (role: {})", recipientRole);
            return;
        }
        if (incident == null) {
            log.warn("Emergency alert email skipped — incident is null");
            return;
        }

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(formatSenderAddress(), "ADRIAN — Emergency Dispatch");
            helper.setTo(toEmail);

            String severity = incident.getSeverity() != null ? incident.getSeverity().toUpperCase() : "HIGH";
            String location = "";
            if (incident.getLocation() != null) {
                location = incident.getLocation().getAddress() != null
                        ? incident.getLocation().getAddress()
                        : "GPS (" + incident.getLocation().getLat() + ", " + incident.getLocation().getLng() + ")";
            }

            String roleLabel = "rescue_team".equals(recipientRole) ? "Rescue Team" : "Hospital Unit";
            String actionText = "rescue_team".equals(recipientRole)
                    ? "Please deploy your team immediately and update status in the ADRIAN system."
                    : "Please prepare emergency triage capacity and update bed availability in the ADRIAN system.";

            helper.setSubject("🚨 NEW EMERGENCY ALERT — " + severity + " — " + location);

            String displayName = (recipientName != null && !recipientName.trim().isEmpty())
                    ? recipientName : roleLabel;

            String typeDisplay = incident.getType() != null
                    ? incident.getType().replace('_', ' ').toUpperCase()
                    : "UNKNOWN";

            String description = incident.getDescription() != null && !incident.getDescription().isBlank()
                    ? incident.getDescription()
                    : "No description provided.";

            String reportedAt = incident.getReportedAt() != null ? incident.getReportedAt() : "Unknown";

            String lat = "N/A";
            String lng = "N/A";
            if (incident.getLocation() != null) {
                lat = String.valueOf(incident.getLocation().getLat());
                lng = String.valueOf(incident.getLocation().getLng());
            }

            String severityColor = switch (severity.toLowerCase()) {
                case "critical" -> "#dc2626";
                case "high"     -> "#ea580c";
                case "medium"   -> "#ca8a04";
                default         -> "#16a34a";
            };

            String htmlContent = """
                <!DOCTYPE html>
                <html>
                <head>
                  <meta charset="utf-8">
                  <meta name="viewport" content="width=device-width, initial-scale=1.0">
                  <title>Emergency Alert</title>
                </head>
                <body style="margin:0;padding:0;background-color:#080d1a;font-family:'Segoe UI',Roboto,Helvetica,Arial,sans-serif;color:#e2e8f0;">
                  <table width="100%%" border="0" cellspacing="0" cellpadding="0" style="background-color:#080d1a;padding:40px 20px;">
                    <tr>
                      <td align="center">
                        <table width="100%%" style="max-width:600px;background-color:#0f172a;border:1px solid #1e293b;border-radius:16px;overflow:hidden;box-shadow:0 10px 25px rgba(0,0,0,0.5);">

                          <!-- Emergency Header Banner -->
                          <tr>
                            <td style="padding:0;background-color:%s;text-align:center;">
                              <div style="padding:16px 32px;">
                                <span style="font-family:monospace;font-size:11px;font-weight:bold;color:rgba(255,255,255,0.8);letter-spacing:2px;text-transform:uppercase;">🚨 ADRIAN EMERGENCY DISPATCH</span>
                                <h1 style="margin:8px 0 4px 0;font-size:24px;font-weight:900;color:#ffffff;letter-spacing:-0.5px;">NEW EMERGENCY ALERT</h1>
                                <span style="display:inline-block;background:rgba(255,255,255,0.2);border-radius:20px;padding:4px 16px;font-size:13px;font-weight:bold;color:#ffffff;">%s — %s</span>
                              </div>
                            </td>
                          </tr>

                          <!-- Greeting -->
                          <tr>
                            <td style="padding:24px 32px 0 32px;">
                              <p style="margin:0;font-size:15px;color:#cbd5e1;line-height:1.5;">
                                Dear <strong style="color:#ffffff;">%s</strong> (%s),
                              </p>
                              <p style="margin:12px 0 0 0;font-size:14px;color:#94a3b8;line-height:1.6;">
                                A new emergency request has been submitted through the <strong style="color:#f1f5f9;">ADRIAN Emergency Response Network</strong>. Immediate action is required.
                              </p>
                            </td>
                          </tr>

                          <!-- Emergency Details Table -->
                          <tr>
                            <td style="padding:20px 32px;">
                              <table width="100%%" style="background-color:#080d1a;border:1px solid #1e293b;border-radius:12px;overflow:hidden;">
                                <tr style="background-color:#0f172a;">
                                  <td colspan="2" style="padding:12px 16px;font-size:11px;font-family:monospace;color:#94a3b8;letter-spacing:1.5px;text-transform:uppercase;border-bottom:1px solid #1e293b;">
                                    Emergency Details
                                  </td>
                                </tr>
                                <tr>
                                  <td style="padding:10px 16px;font-size:12px;color:#64748b;font-weight:600;width:35%%;border-bottom:1px solid #0f172a;">Emergency ID</td>
                                  <td style="padding:10px 16px;font-size:13px;color:#f1f5f9;font-family:monospace;border-bottom:1px solid #0f172a;">%s</td>
                                </tr>
                                <tr>
                                  <td style="padding:10px 16px;font-size:12px;color:#64748b;font-weight:600;border-bottom:1px solid #0f172a;">Type</td>
                                  <td style="padding:10px 16px;font-size:13px;color:#f1f5f9;border-bottom:1px solid #0f172a;">%s</td>
                                </tr>
                                <tr>
                                  <td style="padding:10px 16px;font-size:12px;color:#64748b;font-weight:600;border-bottom:1px solid #0f172a;">Severity</td>
                                  <td style="padding:10px 16px;border-bottom:1px solid #0f172a;">
                                    <span style="background-color:%s;color:white;padding:2px 10px;border-radius:12px;font-size:11px;font-weight:bold;">%s</span>
                                  </td>
                                </tr>
                                <tr>
                                  <td style="padding:10px 16px;font-size:12px;color:#64748b;font-weight:600;border-bottom:1px solid #0f172a;">Location</td>
                                  <td style="padding:10px 16px;font-size:13px;color:#f1f5f9;border-bottom:1px solid #0f172a;">%s</td>
                                </tr>
                                <tr>
                                  <td style="padding:10px 16px;font-size:12px;color:#64748b;font-weight:600;border-bottom:1px solid #0f172a;">Coordinates</td>
                                  <td style="padding:10px 16px;font-size:12px;color:#94a3b8;font-family:monospace;border-bottom:1px solid #0f172a;">%s, %s</td>
                                </tr>
                                <tr>
                                  <td style="padding:10px 16px;font-size:12px;color:#64748b;font-weight:600;border-bottom:1px solid #0f172a;">Reported At</td>
                                  <td style="padding:10px 16px;font-size:13px;color:#f1f5f9;border-bottom:1px solid #0f172a;">%s</td>
                                </tr>
                                <tr>
                                  <td style="padding:10px 16px;font-size:12px;color:#64748b;font-weight:600;">Description</td>
                                  <td style="padding:10px 16px;font-size:13px;color:#f1f5f9;line-height:1.5;">%s</td>
                                </tr>
                              </table>
                            </td>
                          </tr>

                          <!-- Action Required -->
                          <tr>
                            <td style="padding:0 32px 24px 32px;">
                              <div style="background-color:rgba(239,68,68,0.08);border:1px solid rgba(239,68,68,0.25);border-radius:10px;padding:16px;">
                                <p style="margin:0;font-size:13px;color:#fca5a5;line-height:1.6;">
                                  <strong>⚡ Action Required:</strong> %s
                                </p>
                              </div>
                            </td>
                          </tr>

                          <!-- Footer -->
                          <tr>
                            <td style="padding:16px 32px;background-color:#080d1a;border-top:1px solid #1e293b;text-align:center;">
                              <p style="margin:0;font-size:11px;color:#475569;">
                                © 2026 ADRIAN · Autonomous Disaster Response &amp; Integrated Aid Network · Sri Lanka<br>
                                <span style="color:#374151;">This is an automated emergency dispatch notification. Do not reply to this email.</span>
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(
                    severityColor,                      // Header banner color
                    severity, typeDisplay,              // Badge: CRITICAL — FIRE
                    displayName, roleLabel,             // Dear X (Rescue Team)
                    incident.getTrackingCode() != null ? incident.getTrackingCode() : incident.getId(),
                    typeDisplay,
                    severityColor, severity,
                    location,
                    lat, lng,
                    reportedAt,
                    description,
                    actionText
                );

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("✅ Emergency alert email sent to {} ({}) for incident {}",
                    toEmail, recipientRole, incident.getId());

        } catch (Exception e) {
            log.error("❌ Failed to send emergency alert email to {} for incident {}: {}",
                    toEmail, incident != null ? incident.getId() : "unknown", e.getMessage());
            // IMPORTANT: Never re-throw — email failure must NOT affect incident creation
        }
    }
}
