package com.silveira.accounting.application.nyl;

import com.silveira.accounting.models.NylRecord;
import java.nio.file.Path;
import java.util.List;

public interface NylImportGateway {
    ParsedDocument parse(Path pdf);
    ParsedDocument parseWithOcr(Path pdf);
    ParsedDocument parseWithAi(Path pdf);
    double detectDeclaredTotal(String text);
    List<String> validate(List<NylRecord> records, double declaredTotal);

    record ParsedDocument(List<NylRecord> records, String text) {
    }
}
