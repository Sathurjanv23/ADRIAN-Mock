package com.nova.emergency.service;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Value("${spring.mail.username:sathu20030303@gmail.com}")
    private String fromEmail;

    /**
     * Send OTP Verification Email with modern HTML design
     */
    public boolean sendOtpEmail(String toEmail, String otp, String name) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromEmail, "PROJECT NOVA — Emergency Response");
            helper.setTo(toEmail);
            helper.setSubject("PROJECT NOVA — Your Account Verification Code: " + otp);

            String displayName = (name != null && !name.trim().isEmpty()) ? name : "Responder";

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
                        <table width="100%%" max-width="540" style="max-width:540px;background-color:#0f172a;border:1px solid #1e293b;border-radius:16px;overflow:hidden;box-shadow:0 10px 25px rgba(0,0,0,0.5);">
                          
                          <!-- Header -->
                          <tr>
                            <td style="padding:32px 32px 20px 32px;text-align:center;background:linear-gradient(180deg, rgba(0,212,255,0.08) 0%%, rgba(15,23,42,0) 100%%);border-bottom:1px solid #1e293b;">
                              <div style="display:inline-block;padding:8px 16px;background:rgba(0,212,255,0.1);border:1px solid rgba(0,212,255,0.3);border-radius:30px;margin-bottom:12px;">
                                <span style="font-family:monospace;font-size:12px;font-weight:bold;color:#00d4ff;letter-spacing:1.5px;">PROJECT NOVA</span>
                              </div>
                              <h1 style="margin:0;font-size:22px;font-weight:800;color:#ffffff;letter-spacing:-0.5px;">Email Verification</h1>
                              <p style="margin:6px 0 0 0;font-size:13px;color:#94a3b8;">National Emergency Response Network</p>
                            </td>
                          </tr>

                          <!-- Body -->
                          <tr>
                            <td style="padding:32px;">
                              <p style="margin:0 0 16px 0;font-size:15px;color:#cbd5e1;line-height:1.5;">
                                Hello <strong style="color:#ffffff;">%s</strong>,
                              </p>
                              <p style="margin:0 0 24px 0;font-size:14px;color:#94a3b8;line-height:1.6;">
                                Thank you for registering with PROJECT NOVA. To activate your account and verify your email address, please use the 6-digit verification code below:
                              </p>

                              <!-- OTP Box -->
                              <div style="background-color:#080d1a;border:2px dashed #00d4ff;border-radius:12px;padding:24px;text-align:center;margin:24px 0;">
                                <div style="font-size:11px;font-family:monospace;color:#94a3b8;letter-spacing:1px;text-transform:uppercase;margin-bottom:8px;">Your One-Time Password</div>
                                <div style="font-family:'Courier New',Courier,monospace;font-size:36px;font-weight:900;letter-spacing:10px;color:#00d4ff;">%s</div>
                                <div style="font-size:11px;color:#ef4444;margin-top:8px;font-weight:600;">⏱ Valid for 30 seconds only</div>
                              </div>

                              <p style="margin:24px 0 0 0;font-size:12px;color:#64748b;line-height:1.5;">
                                If you did not initiate this registration request, you can safely ignore this email. Do not share this OTP code with anyone.
                              </p>
                            </td>
                          </tr>

                          <!-- Footer -->
                          <tr>
                            <td style="padding:20px 32px;background-color:#080d1a;border-top:1px solid #1e293b;text-align:center;">
                              <p style="margin:0;font-size:11px;color:#475569;">
                                © 2026 PROJECT NOVA · Autonomous Disaster Management System · Sri Lanka
                              </p>
                            </td>
                          </tr>

                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(displayName, otp);

            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("✅ Verification OTP email successfully sent to: {}", toEmail);
            return true;

        } catch (Exception e) {
            log.error("❌ Failed to send OTP email to {}: {}", toEmail, e.getMessage());
            return false;
        }
    }
}
