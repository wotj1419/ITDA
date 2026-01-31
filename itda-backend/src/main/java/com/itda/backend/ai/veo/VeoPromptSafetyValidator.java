package com.itda.backend.ai.veo;

import com.itda.backend.global.exception.BusinessException;
import com.itda.backend.global.response.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component
public class VeoPromptSafetyValidator {

    private static final List<Pattern> CELEBRITY_PATTERNS = List.of(
            Pattern.compile("\\bcelebrity\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bfamous\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\breal\\s+person\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bpublic\\s+figure\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bactor\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bactress\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bsinger\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bidol\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\binfluencer\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bpolitician\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bpresident\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bprime\\s+minister\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\broyal\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bqueen\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bking\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bin\\s+the\\s+style\\s+of\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bportray(?:ing|ed)?\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bstarring\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bfeaturing\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\blike\\s+[A-Z][a-z]+\\s+[A-Z][a-z]+\\b"),
            Pattern.compile("\\b(?:portrait|photo)\\s+of\\s+[A-Z][a-z]+\\s+[A-Z][a-z]+\\b")
    );

    private static final List<Pattern> CELEBRITY_PATTERNS_KO = List.of(
            Pattern.compile("연예인"),
            Pattern.compile("유명인"),
            Pattern.compile("실존\\s*인물"),
            Pattern.compile("실제\\s*인물"),
            Pattern.compile("배우"),
            Pattern.compile("가수"),
            Pattern.compile("아이돌"),
            Pattern.compile("인플루언서"),
            Pattern.compile("정치인"),
            Pattern.compile("대통령"),
            Pattern.compile("총리"),
            Pattern.compile("왕족"),
            Pattern.compile("닮은|같은|처럼")
    );

    private static final List<Pattern> CHILD_PATTERNS = List.of(
            Pattern.compile("\\bchild\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bkid\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\btoddler\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\binfant\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bbaby\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bminor\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("아동|어린이|아이|유아|아기|미성년")
    );

    private static final List<Pattern> SEXUAL_PATTERNS = List.of(
            Pattern.compile("\\bnude\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bnaked\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bsex\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bporn\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\berotic\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("노출|성행위|야한|포르노|에로")
    );

    private static final List<Pattern> VIOLENCE_PATTERNS = List.of(
            Pattern.compile("\\bgore\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bblood\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bmurder\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bkill\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bdecapitat", Pattern.CASE_INSENSITIVE),
            Pattern.compile("잔혹|살인|피|시체")
    );

    private static final List<Pattern> HATE_PATTERNS = List.of(
            Pattern.compile("\\bnazi\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bkkk\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bhate\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("혐오|증오|인종차별")
    );

    private static final List<Pattern> SELF_HARM_PATTERNS = List.of(
            Pattern.compile("\\bsuicide\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("\\bself-harm\\b", Pattern.CASE_INSENSITIVE),
            Pattern.compile("자살|자해")
    );

    public void validate(String prompt) {
        String text = prompt == null ? "" : prompt.trim();
        if (text.isEmpty()) {
            return;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        List<String> violations = new ArrayList<>();

        if (matchesAny(CELEBRITY_PATTERNS, text) || matchesAny(CELEBRITY_PATTERNS_KO, text)) {
            violations.add("celebrity/real person");
        }
        if (matchesAny(CHILD_PATTERNS, text)) {
            violations.add("child/minor");
        }
        if (matchesAny(SEXUAL_PATTERNS, text)) {
            violations.add("sexual content");
        }
        if (matchesAny(VIOLENCE_PATTERNS, normalized)) {
            violations.add("graphic violence");
        }
        if (matchesAny(HATE_PATTERNS, normalized)) {
            violations.add("hate/harassment");
        }
        if (matchesAny(SELF_HARM_PATTERNS, normalized)) {
            violations.add("self-harm");
        }

        if (!violations.isEmpty()) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "Video prompt violates Vertex AI usage guidelines: " + String.join(", ", violations)
                            + ". Please rephrase using fictional characters and non-sensitive content."
            );
        }
    }

    private boolean matchesAny(List<Pattern> patterns, String text) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (Pattern pattern : patterns) {
            if (pattern.matcher(text).find()) {
                return true;
            }
        }
        return false;
    }
}
