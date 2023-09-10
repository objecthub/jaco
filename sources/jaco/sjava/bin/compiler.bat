%JC% -d %CLASSES% .\struct\*.java .\component\*.java .\context\*.java .\grammar\*.java .\Main.java
copy .\grammar\Parser.tables %CLASSES%\jaco\sjava\grammar
copy .\resources %CLASSES%\jaco\sjava\resources
