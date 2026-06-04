# Silveira Financial Group Accounting

Aplicación de escritorio JavaFX para importar extractos bancarios y resúmenes de New York Life, revisar datos antes de guardarlos, consultar, conciliar y exportar reportes.

## Requisitos

- Java 21
- Maven 3.9+
- Windows 10/11

En este equipo hay un JDK 21 en:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot'
$env:Path="$env:JAVA_HOME\bin;" + $env:Path
```

## Ejecutar

```powershell
mvn javafx:run
```

Si no tienes Maven instalado, este proyecto ya incluye una copia local descargada en `.tools` durante el desarrollo:

```powershell
$env:JAVA_HOME='C:\Program Files\Eclipse Adoptium\jdk-21.0.10.7-hotspot'
$env:Path="$env:JAVA_HOME\bin;" + $env:Path
.\.tools\apache-maven-3.9.9\bin\mvn.cmd "-Dmaven.repo.local=$PWD\.m2\repository" javafx:run
```

La base de datos local se crea automáticamente en:

```text
data/silveira-accounting.db
```

La app funciona sin internet después de descargar dependencias la primera vez.

## Empaquetar para Windows

1. Generar el jar:

```powershell
mvn clean package
```

2. Crear una app nativa con `jpackage`:

```powershell
jpackage --type exe --name "Silveira Accounting" --input target --main-jar silveira-accounting-1.0.0.jar --main-class com.silveira.accounting.MainApp --dest dist --icon img/logo.ico
```

Si no tienes `logo.ico`, puedes omitir `--icon` o convertir `img/logo.png` a `.ico`.

## PDFs de prueba

Los parsers están pensados para probarse con:

- `C:\Users\letic_vi9opz0\Desktop\Work\Accounting\Banco\03_26.pdf`
- `C:\Users\letic_vi9opz0\Desktop\Work\Accounting\NewYorkLife\HastaAbril2026.pdf`

## Diseño de importación

- Banco: detecta fecha, descripción, importe, tipo de movimiento, proveedor, referencia, mes/año y PDF origen.
- New York Life: guarda conceptos dinámicos por mes/año. Si un PDF nuevo vuelve a traer meses anteriores, los registros ya existentes se ignoran por huella lógica y no se sobrescriben.
- Antes de guardar siempre se abre una pantalla de revisión para confirmar, corregir o cancelar.
- PDFBox es la vía principal. Si el PDF trae texto real, se importa como `importado_auto`.
- Si un PDF viene escaneado como imagen y no trae texto embebido, la app intenta OCR local con Tesseract. Los registros OCR quedan como `ocr_revisado`, marcados para revisión y nunca se guardan sin confirmación.
- La entrada manual queda marcada como `manual`.
- La app guarda PDF origen, estado de importación, alertas de revisión e historial de correcciones.
- Si un mes NYL está cerrado en la tabla `closed_months`, la app bloquea nuevas escrituras para ese mes.

## OCR local

Para leer PDFs escaneados necesitas instalar Tesseract OCR para Windows.

La app busca `tesseract.exe` en:

```text
C:\Program Files\Tesseract-OCR\tesseract.exe
C:\Program Files (x86)\Tesseract-OCR\tesseract.exe
PATH del sistema
```

El OCR se usa solo para prellenar datos. En contabilidad, la revisión humana sigue siendo obligatoria.

## Estructura

```text
src/main/java/com/silveira/accounting
  ui
  controllers
  services
  parsers
  repositories
  database
  models
  utils
src/main/resources/img
```
