package com.stockpulse.stockpulse.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Getter
@NoArgsConstructor
@Table(name = "stocks",
        uniqueConstraints = @UniqueConstraint(columnNames = {"keyword", "email"}))
public class Stock {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // 여러 사람이 같은 키워드를 구독할 수 있어야 하므로, 고유값은 키워드 단독이 아니라 (키워드, 이메일) 조합이다.
    @Column(nullable = false)
    private String keyword;

    @Column(nullable = false)
    private String email;

    // 이메일 확인(더블 옵트인)과 구독 해지 링크에 함께 사용하는 토큰. 공개 화면에는 노출되지 않는다.
    // DB 레벨 nullable=false로 두지 않은 이유: 기존에 저장된 row(토큰 없음)가 있어도
    // 배포 시 스키마 마이그레이션이 실패하지 않게 하기 위함. 새로 생성되는 Stock은
    // 생성자에서 항상 토큰을 채우므로 애플리케이션 로직상으로는 실질적으로 필수값이다.
    @Column(unique = true)
    private String token;

    // 확인 메일의 링크를 눌러야 true가 되고, 그 전까지는 다이제스트가 발송되지 않는다.
    @Setter
    private boolean confirmed = false;

    // 같은 회차(같은 최신 기사 묶음)를 중복 발송하지 않기 위한 기록
    @Setter
    private String lastSentArticleLink;

    @Setter
    private LocalDateTime lastSentAt;

    @Setter
    private boolean lastSendSuccess = true;

    public Stock(String keyword, String email) {
        this.keyword = keyword;
        this.email = email;
        this.token = UUID.randomUUID().toString();
    }
}