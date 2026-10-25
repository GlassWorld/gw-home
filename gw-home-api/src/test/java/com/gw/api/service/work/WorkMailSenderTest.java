package com.gw.api.service.work;

import com.gw.api.config.WorkNotificationConfig;
import com.gw.api.config.WorkNotificationProperties;
import com.gw.share.jvo.work.WorkReminderTargetJvo;
import com.gw.share.jvo.work.WorkTodoListJvo;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.List;
import java.time.LocalDate;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import org.springframework.mail.MailSendException;
import static org.junit.jupiter.api.Assertions.*;

class WorkMailSenderTest {
    @Test void digestContainsClickableOwnedTodoLinksAndEscapedText() {
        var content=new WorkNotificationMessage(properties(1025)).digest(List.of(WorkTodoListJvo.builder()
                .uuid("todo").workUnitUuid("unit").ttl("<b>title</b>").dscr("<img src=x>")
                .workUnitTtl("업무").todoGroup("TODAY").build()),LocalDate.parse("2026-10-25"),"Asia/Seoul");
        assertTrue(content.html().contains("<a href=\"https://example.test/work/todos/unit?todo=todo\">할 일 상세 열기</a>"));
        assertFalse(content.html().contains("<b>title")); assertFalse(content.html().contains("<img"));
        assertTrue(content.text().contains("오늘 할 일 · 표시 1건"));
    }
    @Test void actualMimeMessageIsAcceptedAndContainsNoExecutableTodoHtml() throws Exception {
        try(var smtp=new LocalSmtp("250 accepted")) {
            var properties=properties(smtp.port());
            properties.setFromName("정종태");
            var content=new WorkNotificationMessage(properties).reminder(WorkReminderTargetJvo.builder()
                    .ttl("<script>alert('x')</script>").dscr("<img src=x onerror=alert(1)>")
                    .workUnitTtl("업무").uuid("todo-uuid").workUnitUuid("unit-uuid").build());
            assertFalse(content.html().contains("<script>")); assertFalse(content.html().contains("<img"));
            assertTrue(content.html().contains("href=\"https://example.test/work/todos/unit-uuid?todo=todo-uuid\""));
            var result=sender(properties).send("me@example.test",content,"delivery-key");
            assertEquals(WorkMailSender.Outcome.SENT,result.outcome());
            String wire=smtp.message.get(5,TimeUnit.SECONDS);
            var mime=new MimeMessage(Session.getInstance(new Properties()),new ByteArrayInputStream(wire.getBytes(StandardCharsets.UTF_8)));
            assertEquals("me@example.test",mime.getAllRecipients()[0].toString());
            assertEquals("정종태",((jakarta.mail.internet.InternetAddress)mime.getFrom()[0]).getPersonal());
            assertEquals("[Glass World] 할 일 알림",mime.getSubject());
            assertTrue(mime.isMimeType("multipart/*"));
            assertEquals("<delivery-key@gw-home.notification>",mime.getMessageID());
        }
    }
    @Test void smtpTemporaryRejectionIsRetryable() throws Exception { assertSmtpResult("451 later",WorkMailSender.Outcome.RETRY); }
    @Test void smtpPermanentRejectionIsFinal() throws Exception { assertSmtpResult("550 rejected",WorkMailSender.Outcome.FAILED); }
    @Test void connectionLostAfterDataIsUnknownAndMustNotRetry() throws Exception { assertSmtpResult(null,WorkMailSender.Outcome.UNKNOWN); }
    @Test void connectionRefusedIsRetryableButTimeoutIsUnknown() {
        assertEquals(WorkMailSender.Outcome.RETRY,WorkMailSender.classify(new MailSendException("failed",new ConnectException())).outcome());
        assertEquals(WorkMailSender.Outcome.UNKNOWN,WorkMailSender.classify(new MailSendException("failed",new SocketTimeoutException())).outcome());
    }
    private void assertSmtpResult(String response,WorkMailSender.Outcome outcome) throws Exception {
        try(var smtp=new LocalSmtp(response)) {
            assertEquals(outcome,sender(properties(smtp.port())).send("me@example.test",new WorkMailSender.Content("알림","본문","<p>본문</p>"),"key").outcome());
            smtp.message.get(5,TimeUnit.SECONDS);
        }
    }
    private WorkMailSender sender(WorkNotificationProperties properties) {
        return new WorkMailSender(new WorkNotificationConfig().notificationMailSender(properties),properties);
    }
    private WorkNotificationProperties properties(int port) {
        var properties=new WorkNotificationProperties();
        properties.setEnabled(true); properties.setSmtpHost("127.0.0.1"); properties.setSmtpPort(port);
        properties.setStarttls(false); properties.setFrom("sender@example.test"); properties.setAppUrl("https://example.test");
        return properties;
    }
    // 테스트 프로세스의 루프백 SMTP에서만 접수·거부·응답 유실을 재현한다.
    private static class LocalSmtp implements AutoCloseable {
        final ServerSocket server=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"));
        final ExecutorService executor=Executors.newSingleThreadExecutor();
        final Future<String> message;
        LocalSmtp(String finalResponse) throws IOException {
            server.setSoTimeout(5000);
            message=executor.submit(() -> {
                try(Socket socket=server.accept()) {
                    socket.setSoTimeout(5000);
                    var reader=new BufferedReader(new InputStreamReader(socket.getInputStream(),StandardCharsets.UTF_8));
                    var writer=new PrintWriter(new OutputStreamWriter(socket.getOutputStream(),StandardCharsets.UTF_8),true);
                    writer.print("220 localhost test\r\n"); writer.flush();
                    StringBuilder data=new StringBuilder();
                    boolean readingData=false;
                    for(String line;(line=reader.readLine())!=null;) {
                        if(readingData) {
                            if(line.equals(".")) {
                                if(finalResponse==null) return data.toString();
                                writer.print(finalResponse+"\r\n"); writer.flush(); readingData=false;
                            } else data.append(line).append("\r\n");
                        } else if(line.startsWith("DATA")) {
                            writer.print("354 send\r\n"); writer.flush(); readingData=true;
                        } else if(line.startsWith("QUIT")) {
                            writer.print("221 bye\r\n"); writer.flush(); return data.toString();
                        } else {
                            writer.print("250 OK\r\n"); writer.flush();
                        }
                    }
                    return data.toString();
                }
            });
        }
        int port() { return server.getLocalPort(); }
        @Override public void close() throws IOException { server.close(); executor.shutdownNow(); }
    }
}
