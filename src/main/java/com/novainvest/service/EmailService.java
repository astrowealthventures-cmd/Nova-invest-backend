package com.novainvest.service;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
@Service
@RequiredArgsConstructor
public class EmailService {

    private final RestTemplate restTemplate;

    @Value("${app.brevo.api-key}")
    private String brevoApiKey;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    // ─────────────────────── Brand tokens (match the frontend) ───────────────────────
    private static final String BRAND_RED      = "#C8102E";
    private static final String BRAND_RED_HOV  = "#E01B3D";
    private static final String BG_PAGE        = "#050505";
    private static final String BG_CARD        = "#101010";
    private static final String BORDER         = "rgba(255,255,255,0.08)";
    private static final String TEXT_PRIMARY   = "#FFFFFF";
    private static final String TEXT_SECONDARY = "#A1A1AA"; // zinc-400
    private static final String TEXT_MUTED     = "#71717A"; // zinc-500
    private static final String FONT_SANS =
            "ui-sans-serif, -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif";
    private static final String FONT_MONO =
            "ui-monospace, 'JetBrains Mono', SFMono-Regular, Menlo, Consolas, monospace";

    // ═══════════════════════════════════════════════════════════════════════════════
    //  Public API
    // ═══════════════════════════════════════════════════════════════════════════════

    public void sendVerificationEmail(String to, String token) {
        String link = frontendUrl + "/verify-email?token=" + token;

        String body = sectionHeader("Verify your email") +
                paragraph("Thanks for signing up for AstroWealthVentures. "
                        + "Confirm your email to activate your account.") +
                button("Verify email address", link) +
                divider() +
                smallNote("This link expires in 24 hours. If you didn't create an account, "
                        + "you can safely ignore this email.") +
                linkBlock(link);

        send(to, "Verify your AstroWealthVentures account", body);
    }

    public void sendForgottenPasswordOtp(String to, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;

        String body = sectionHeader("Reset your password") +
                paragraph("We received a request to reset the password for your "
                        + "AstroWealthVentures account. Click below to set a new one.") +
                button("Reset password", link) +
                divider() +
                smallNote("This link expires in 15 minutes. If you didn't request this, "
                        + "your account is still safe — no action needed.") +
                linkBlock(link);

        send(to, "Reset your AstroWealthVentures password", body);
    }

    public void sendDepositReceived(String email, double amountUsd) {
        String formatted = String.format("$%,.2f", amountUsd);

        String body = sectionHeader("Deposit confirmed 🎉") +
                paragraph("Your deposit has been received and credited to your "
                        + "AstroWealthVentures account. Thanks for investing with us.") +
                "<table role=\"presentation\" width=\"100%\" cellspacing=\"0\" cellpadding=\"0\" "
                + "style=\"margin: 28px 0;\">"
                + "<tr><td align=\"center\" style=\"background:" + BG_CARD
                + "; border:1px solid " + BORDER
                + "; border-radius:16px; padding:28px 24px;\">"
                + "<div style=\"font-family:" + FONT_SANS + "; font-size:11px; letter-spacing:0.25em; "
                + "text-transform:uppercase; color:" + TEXT_MUTED + ";\">Amount credited</div>"
                + "<div style=\"font-family:" + FONT_MONO + "; font-size:40px; font-weight:700; "
                + "color:" + BRAND_RED + "; margin-top:8px;\">" + formatted + "</div>"
                + "</td></tr></table>" +
                button("Open dashboard", frontendUrl + "/dashboard/overview") +
                divider() +
                smallNote("This transaction has been recorded in your activity log. "
                        + "Your funds are now available to invest.");

        send(email, "AstroWealthVentures Deposit Confirmed", body);
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    //  Shared sender (DRY — all three methods delegate here)
    // ═══════════════════════════════════════════════════════════════════════════════

    private void send(String to, String subject, String htmlBody) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("api-key", brevoApiKey);
        headers.set("Content-Type", "application/json");
        headers.set("Accept", "application/json");

        Map<String, Object> body = new HashMap<>();
        body.put("sender", Map.of(
                "name", "AstroWealthVentures",
                "email", "astrowealthventures@gmail.com"
        ));
        body.put("replyTo", Map.of(
                "name", "AstroWealthVentures",
                "email", "astrowealthventures@gmail.com"
        ));
        body.put("to", List.of(Map.of("email", to)));
        body.put("subject", subject);
        body.put("htmlContent", buildShell(htmlBody));
        body.put("textContent", stripHtml(htmlBody)); // accessible plain-text fallback
        body.put("tags", List.of("transactional"));

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

        try {
            restTemplate.postForEntity(BREVO_API_URL, request, String.class);
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            throw new RuntimeException(
                    "Failed to send email to " + to + ": " + e.getResponseBodyAsString(), e);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════════════
    //  Template helpers (build the dark-themed shell + content blocks)
    // ═══════════════════════════════════════════════════════════════════════════════

    /** Outer dark wrapper + card, used by every email. */
    private String buildShell(String content) {
        return "<!DOCTYPE html>\n" +
                "<html lang=\"en\">\n" +
                "<head>\n" +
                "  <meta charset=\"UTF-8\" />\n" +
                "  <meta name=\"viewport\" content=\"width=device-width, initial-scale=1\" />\n" +
                "  <meta name=\"color-scheme\" content=\"dark\" />\n" +
                "  <meta name=\"supported-color-schemes\" content=\"dark\" />\n" +
                "  <title>AstroWealthVentures</title>\n" +
                "</head>\n" +
                "<body style=\"margin:0; padding:0; background:" + BG_PAGE + "; "
                + "font-family:" + FONT_SANS + "; -webkit-font-smoothing:antialiased;\">\n" +
                "  <!-- Preheader (hidden preview text) -->\n" +
                "  <div style=\"display:none; max-height:0; overflow:hidden; font-size:1px; line-height:1px; color:"
                + BG_PAGE + ";\">AstroWealthVentures · Wealth, engineered for the next decade.</div>\n" +
                "\n" +
                "  <table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"background:" + BG_PAGE + ";\">\n" +
                "    <tr>\n" +
                "      <td align=\"center\" style=\"padding: 32px 16px;\">\n" +
                "\n" +
                "        <!-- Card -->\n" +
                "        <table role=\"presentation\" width=\"600\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"max-width:600px; width:100%; background:" + BG_CARD + "; "
                + "border:1px solid " + BORDER + "; border-radius:24px; overflow:hidden;\">\n" +
                "\n" +
                "          <!-- Top accent bar -->\n" +
                "          <tr>\n" +
                "            <td style=\"height:3px; background:linear-gradient(90deg, "
                + "transparent 0%, " + BRAND_RED + " 50%, transparent 100%); font-size:0; line-height:0;\">&nbsp;</td>\n" +
                "          </tr>\n" +
                "\n" +
                "          <!-- Logo -->\n" +
                "          <tr>\n" +
                "            <td align=\"center\" style=\"padding: 36px 32px 8px;\">\n" +
                "              <div style=\"font-family:" + FONT_SANS + "; font-size:22px; font-weight:700; letter-spacing:0.12em;\">\n" +
                "                <span style=\"color:" + TEXT_PRIMARY + ";\">ASTRO</span><span style=\"color:" + BRAND_RED + ";\">WEALTH</span>\n" +
                "              </div>\n" +
                "              <div style=\"font-family:" + FONT_SANS + "; font-size:10px; letter-spacing:0.4em; "
                + "color:" + TEXT_MUTED + "; margin-top:6px; text-transform:uppercase;\">Ventures</div>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "\n" +
                "          <!-- Main content -->\n" +
                "          <tr>\n" +
                "            <td style=\"padding: 24px 32px 32px; color:" + TEXT_PRIMARY + "; font-size:15px; line-height:1.6;\">\n" +
                "              " + content + "\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "\n" +
                "          <!-- Footer -->\n" +
                "          <tr>\n" +
                "            <td style=\"padding: 24px 32px 32px; border-top:1px solid " + BORDER
                + "; font-family:" + FONT_SANS + "; font-size:12px; color:" + TEXT_MUTED + "; line-height:1.6;\">\n" +
                "              <div style=\"text-align:center;\">\n" +
                "                <div style=\"margin-bottom:8px;\">\n" +
                "                  <a href=\"" + frontendUrl + "\" style=\"color:" + TEXT_SECONDARY + "; text-decoration:none;\">Dashboard</a>\n" +
                "                  <span style=\"color:" + TEXT_MUTED + "; padding:0 8px;\">·</span>\n" +
                "                  <a href=\"" + frontendUrl + "/dashboard/plans\" style=\"color:" + TEXT_SECONDARY + "; text-decoration:none;\">Plans</a>\n" +
                "                  <span style=\"color:" + TEXT_MUTED + "; padding:0 8px;\">·</span>\n" +
                "                  <a href=\"" + frontendUrl + "\" style=\"color:" + TEXT_SECONDARY + "; text-decoration:none;\">Support</a>\n" +
                "                </div>\n" +
                "                <div style=\"color:" + TEXT_MUTED + ";\">\n" +
                "                  © " + java.time.Year.now().getValue() + " AstroWealthVentures. All rights reserved.<br/>\n" +
                "                  Investing today. Building tomorrow.\n" +
                "                </div>\n" +
                "              </div>\n" +
                "            </td>\n" +
                "          </tr>\n" +
                "\n" +
                "        </table>\n" +
                "      </td>\n" +
                "    </tr>\n" +
                "  </table>\n" +
                "</body>\n" +
                "</html>";
    }

    private String sectionHeader(String title) {
        return "<h1 style=\"margin:0 0 16px; font-family:" + FONT_SANS
                + "; font-size:26px; font-weight:700; color:" + TEXT_PRIMARY
                + "; line-height:1.2; letter-spacing:-0.01em;\">" + escape(title) + "</h1>";
    }

    private String paragraph(String text) {
        return "<p style=\"margin:0 0 16px; color:" + TEXT_SECONDARY
                + "; font-size:15px; line-height:1.65;\">" + escape(text) + "</p>";
    }

    /** Red pill button — bulletproof across clients (uses both <a> styles and <td> padding). */
    private String button(String label, String href) {
        return "<table role=\"presentation\" cellspacing=\"0\" cellpadding=\"0\" border=\"0\" "
                + "style=\"margin: 24px 0;\">\n"
                + "  <tr>\n"
                + "    <td align=\"center\" style=\"border-radius:12px; background:" + BRAND_RED + ";\">\n"
                + "      <!--[if mso]>\n"
                + "      <v:roundrect xmlns:v=\"urn:schemas-microsoft-com:vml\" xmlns:w=\"urn:schemas-microsoft-com:office:word\" href=\""
                + href + "\" style=\"height:48px;v-text-anchor:middle;width:240px;\" arcsize=\"25%\" stroke=\"f\" fillcolor=\""
                + BRAND_RED + "\">\n"
                + "        <w:anchorlock/>\n"
                + "        <center style=\"color:#ffffff;font-family:sans-serif;font-size:15px;font-weight:bold;\">"
                + escape(label) + "</center>\n"
                + "      </v:roundrect>\n"
                + "      <![endif]-->\n"
                + "      <![if !mso]><!-->\n"
                + "      <a href=\"" + href + "\" style=\"display:inline-block; padding:14px 28px; "
                + "font-family:" + FONT_SANS + "; font-size:15px; font-weight:600; color:#ffffff; "
                + "text-decoration:none; border-radius:12px; background:" + BRAND_RED
                + "; mso-padding-alt:0;\">" + escape(label) + "</a>\n"
                + "      <!--<![endif]-->\n"
                + "    </td>\n"
                + "  </tr>\n"
                + "</table>";
    }

    /** Small muted block for expiry / warning notes. */
    private String smallNote(String text) {
        return "<p style=\"margin:0; color:" + TEXT_MUTED
                + "; font-size:13px; line-height:1.55;\">" + escape(text) + "</p>";
    }

    /** Raw link as a fallback under the button (for clients that strip button styling). */
    private String linkBlock(String href) {
        return "<p style=\"margin:16px 0 0; word-break:break-all; font-family:" + FONT_MONO
                + "; font-size:12px; color:" + TEXT_MUTED + ";\">"
                + "Or copy this link: <span style=\"color:" + TEXT_SECONDARY + ";\">" + escape(href) + "</span></p>";
    }

    private String divider() {
        return "<table role=\"presentation\" width=\"100%\" cellpadding=\"0\" cellspacing=\"0\" "
                + "style=\"margin: 24px 0;\">\n"
                + "  <tr><td style=\"border-top:1px solid " + BORDER + "; font-size:0; line-height:0;\">&nbsp;</td></tr>\n"
                + "</table>";
    }

    /** Basic HTML-entity escape for user-supplied text. */
    private String escape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }

    /** Crude HTML stripper for the plain-text fallback. */
    private String stripHtml(String html) {
        if (html == null) return "";
        return html.replaceAll("<[^>]+>", " ")
                .replaceAll("&nbsp;", " ")
                .replaceAll("&amp;", "&")
                .replaceAll("&lt;", "<")
                .replaceAll("&gt;", ">")
                .replaceAll("\\s+", " ")
                .trim();
    }
}