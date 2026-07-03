package com.silveira.accounting.application.importing;

import java.nio.file.Path;

public interface DocumentImportGateway<T> {
    T importPdf(Path pdf);
}
