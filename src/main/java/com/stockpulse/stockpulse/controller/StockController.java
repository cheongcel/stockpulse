package com.stockpulse.stockpulse.controller;

import com.stockpulse.stockpulse.domain.Stock;
import com.stockpulse.stockpulse.repository.StockRepository;
import com.stockpulse.stockpulse.service.DigestService;
import com.stockpulse.stockpulse.service.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.regex.Pattern;

@Controller
@RequiredArgsConstructor
public class StockController {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[\\w.+-]+@[\\w-]+\\.[a-zA-Z]{2,}$");
    private static final int MAX_KEYWORD_LENGTH = 30;

    private final StockRepository stockRepository;
    private final DigestService digestService;
    private final EmailService emailService;

    @GetMapping("/")
    public String index() {
        // 공개 화면에는 전체 구독자 목록을 노출하지 않는다 (소유자 구분이 없는 문제 방지).
        return "index";
    }

    @PostMapping("/subscribe")
    public String subscribe(@RequestParam String keyword,
                            @RequestParam String email,
                            RedirectAttributes redirectAttributes) {
        String cleanKeyword = keyword == null ? "" : keyword.trim();
        String cleanEmail = email == null ? "" : email.trim();

        if (cleanKeyword.isEmpty() || cleanKeyword.length() > MAX_KEYWORD_LENGTH) {
            redirectAttributes.addFlashAttribute("successMsg",
                    "키워드는 1자 이상 " + MAX_KEYWORD_LENGTH + "자 이하로 입력해주세요.");
            return "redirect:/";
        }
        if (!EMAIL_PATTERN.matcher(cleanEmail).matches()) {
            redirectAttributes.addFlashAttribute("successMsg", "올바른 이메일 형식이 아니에요.");
            return "redirect:/";
        }

        if (stockRepository.existsByKeywordAndEmail(cleanKeyword, cleanEmail)) {
            redirectAttributes.addFlashAttribute("successMsg", "이미 구독 중인 키워드예요.");
            return "redirect:/";
        }

        Stock stock = stockRepository.save(new Stock(cleanKeyword, cleanEmail));
        emailService.sendConfirmationEmail(cleanEmail, cleanKeyword, stock.getToken());
        redirectAttributes.addFlashAttribute("successMsg",
                "확인 메일을 보냈어요. 메일함에서 링크를 눌러야 구독이 시작돼요.");
        return "redirect:/";
    }

    @GetMapping("/confirm/{token}")
    public String confirm(@PathVariable String token, RedirectAttributes redirectAttributes) {
        stockRepository.findByToken(token).ifPresentOrElse(stock -> {
            stock.setConfirmed(true);
            stockRepository.save(stock);
            redirectAttributes.addFlashAttribute("successMsg",
                    stock.getKeyword() + " 키워드 구독이 확인됐어요!");
        }, () -> redirectAttributes.addFlashAttribute("successMsg", "유효하지 않은 링크예요."));
        return "redirect:/";
    }

    // 해지는 이메일 속 개인 전용 링크(토큰)로만 가능하다. 공개 화면에는 삭제 버튼이 없다.
    @GetMapping("/unsubscribe/{token}")
    public String unsubscribe(@PathVariable String token, RedirectAttributes redirectAttributes) {
        stockRepository.findByToken(token).ifPresentOrElse(stock -> {
            String keyword = stock.getKeyword();
            stockRepository.delete(stock);
            redirectAttributes.addFlashAttribute("successMsg", keyword + " 구독이 취소됐어요.");
        }, () -> redirectAttributes.addFlashAttribute("successMsg", "이미 해지되었거나 유효하지 않은 링크예요."));
        return "redirect:/";
    }

    @GetMapping("/analyze/{keyword}")
    public String analyze(@PathVariable String keyword, Model model) {
        DigestService.Digest digest = digestService.getDigest(keyword);
        model.addAttribute("keyword", keyword);
        model.addAttribute("newsList", digest.articles);
        return "result";
    }
}