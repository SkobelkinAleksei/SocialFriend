package org.example.user.dto.report;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReportRequest {

    @NotBlank(message = "Укажите категорию")
    @Size(max = 20)
    private String category;

    @NotBlank(message = "Укажите причину")
    @Size(max = 20)
    private String reason;

    @NotNull(message = "Укажите, на кого жалоба")
    @Positive
    private Long accusedId;

    @NotNull(message = "Укажите цель жалобы")
    @Positive
    private Long targetId;

    private Long roomId;

    @Size(max = 250)
    private String targetTitle;

    @Size(max = 2000)
    private String snapshotText;

    @Size(max = 500, message = "Опишите причину короче 500 символов")
    private String details;
}
