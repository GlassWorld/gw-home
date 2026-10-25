package com.gw.api.service.work;

import com.gw.api.config.WorkNotificationProperties;
import com.gw.share.jvo.work.*;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

@Component @RequiredArgsConstructor
public class WorkNotificationMessage {
    private final WorkNotificationProperties properties;

    // 본문은 저장하지 않고 발송 시점의 현재 TODO로 구성한다.
    public WorkMailSender.Content reminder(WorkReminderTargetJvo todo) {
        String text=todo.getTtl()+"\n업무: "+todo.getWorkUnitTtl()+"\n"+schedule(todo.getStrtDt(),todo.getDueDt())
                +"\n\n"+excerpt(todo.getDscr())+"\n\n할 일 열기: "+link(todo.getWorkUnitUuid(),todo.getUuid());
        return content("[Glass World] 할 일 알림",text,link(todo.getWorkUnitUuid(),todo.getUuid()));
    }

    // 서로 배타적인 그룹으로 분류하며 모든 사용자 입력을 HTML 이스케이프한다.
    public WorkMailSender.Content digest(List<WorkTodoListJvo> todos,LocalDate today,String zone) {
        StringBuilder text=new StringBuilder("내 할 일 요약 · "+today+" · "+zone+"\n");
        List<WorkTodoListJvo> shown=todos.stream().limit(200).toList();
        for (String group:List.of("OVERDUE","TODAY","UPCOMING")) {
            List<WorkTodoListJvo> items=shown.stream().filter(todo -> group.equals(todo.getTodoGroup())).toList();
            String title=switch(group) { case "OVERDUE" -> "지연된 일"; case "TODAY" -> "오늘 할 일"; default -> "3일 이내 마감 예정"; };
            text.append("\n").append(title).append(" · 표시 ").append(items.size()).append("건\n");
            for (WorkTodoListJvo todo:items) {
                text.append("\n").append(todo.getTtl()).append(" · ").append(todo.getWorkUnitTtl())
                    .append("\n").append(schedule(todo.getStrtDt(),todo.getDueDt())).append("\n").append(excerpt(todo.getDscr()))
                    .append("\n").append(link(todo.getWorkUnitUuid(),todo.getUuid())).append("\n");
            }
        }
        if (todos.size()>200) text.append("\n최대 200건을 표시했습니다. 전체 목록은 내 할 일에서 확인하세요.\n");
        WorkMailSender.Content message=content("[Glass World] "+today+" 내 할 일 요약",text.toString(),baseUrl()+"/work/todos");
        String html=message.html();
        // 본문에서 직접 만든 상세 URL만 링크로 바꾼다. 사용자 입력 URL은 링크로 해석하지 않는다.
        for (WorkTodoListJvo todo:shown) {
            String escapedUrl=HtmlUtils.htmlEscape(link(todo.getWorkUnitUuid(),todo.getUuid()));
            html=html.replace("<br>"+escapedUrl+"<br>","<br><a href=\""+escapedUrl+"\">할 일 상세 열기</a><br>");
        }
        return new WorkMailSender.Content(message.subject(),message.text(),html);
    }

    private WorkMailSender.Content content(String subject,String text,String link) {
        String html="<html><body><div style=\"font-family:sans-serif;line-height:1.6\">"
                +HtmlUtils.htmlEscape(text).replace("\n","<br>")
                +"<p><a href=\""+HtmlUtils.htmlEscape(link)+"\">내 할 일 열기</a></p></div></body></html>";
        return new WorkMailSender.Content(subject,text,html);
    }
    private String link(String unit,String todo) { return baseUrl()+"/work/todos/"+unit+"?todo="+todo; }
    private String baseUrl() { return properties.getAppUrl().replaceAll("/+$",""); }
    private String schedule(LocalDate start,LocalDate due) { return "시작: "+(start==null?"미정":start)+" / 마감: "+(due==null?"미정":due); }
    private String excerpt(String description) { return description==null?"":description.substring(0,Math.min(description.length(),2000)); }
}
