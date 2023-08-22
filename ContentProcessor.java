import java.io.BufferedReader;
import java.io.FileReader;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;
import java.nio.file.*;

public class ContentProcessor {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Insufficient argument. Pass one argument as txt file path");
            return;
        }
        
        String filePath = args[0];
        File file = new File(filePath);
        
        if (!file.exists()) {
            System.out.println("File does not exist.");
            return;
        } else if (!file.isFile()) {
            System.out.println("Specified path is not a file.");
            return;
        } else if (!filePath.toLowerCase().endsWith(".txt")) {
            System.out.println("File is not a .txt file.");
            return;
        }

        int chunkSize = Runtime.getRuntime().availableProcessors(); // Use active processors as chunk size

        try {
            long fileSize = Files.size(Paths.get(filePath));
            long chunkOffset = fileSize / chunkSize;
            
            ExecutorService executor = Executors.newFixedThreadPool(chunkSize);
            List<Future<Content>> results = new ArrayList<>();

            for (int i = 0; i < chunkSize; i++) {
                long startOffset = i * chunkOffset;
                long endOffset = (i == chunkSize - 1) ? fileSize : (i + 1) * chunkOffset;

                Callable<Content> task = new ChunkProcessor(filePath, startOffset, endOffset);
                results.add(executor.submit(task));
            }

            int totalLines = 0;
            int totalSpaces = 0;
            int totalTabs = 0;
            Map<Character, Integer> specialCharCounts = new HashMap<>();

            for (Future<Content> resultFuture : results) {
                Content result = resultFuture.get();
                totalLines += result.getTotalLines();
                totalSpaces += result.getTotalSpaces();
                totalTabs += result.getTotalTabs();
                for (Map.Entry<Character, Integer> entry : result.getSpecialCharCounts().entrySet()) {
                    specialCharCounts.merge(entry.getKey(), entry.getValue(), Integer::sum);
                }
            }

            // Print results
            System.out.println("Total number of lines = " + totalLines);
            System.out.println("Total number of spaces = " + totalSpaces);
            System.out.println("Total number of tabs = " + totalTabs);
            for (Map.Entry<Character, Integer> entry : specialCharCounts.entrySet()) {
                System.out.println("Total number of " + entry.getKey() + " character = " + entry.getValue());
            }

            executor.shutdown();
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }
    }
}

class ChunkProcessor implements Callable<Content> {
    private final String filePath;
    private final long startOffset;
    private final long endOffset;

    public ChunkProcessor(String filePath, long startOffset, long endOffset) {
        this.filePath = filePath;
        this.startOffset = startOffset;
        this.endOffset = endOffset;
    }

    @Override
    public Content call() {
        int linesProcessed = 0;
        int totalSpaces = 0;
        int totalTabs = 0;
        Map<Character, Integer> specialCharCounts = new HashMap<>();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            long currentOffset = 0;

            while ((line = reader.readLine()) != null) {
                if (currentOffset >= startOffset && currentOffset < endOffset) {
                    char[] chars = line.toCharArray();
                    int lineSpaces = 0;
                    int lineTabs = 0;

                    for (char c : chars) {
                        if (c == ' ') {
                            lineSpaces++;
                        } else if (c == '\t') {
                            lineTabs++;
                        } else if (!Character.isLetterOrDigit(c)) {
                            specialCharCounts.merge(c, 1, Integer::sum);
                        }
					}

                    totalSpaces += lineSpaces;
                    totalTabs += lineTabs;
                    linesProcessed++;
                }

				// calculate offset in file
                currentOffset += line.length() + System.lineSeparator().length();

                if (currentOffset >= endOffset) {
                    break;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new Content(linesProcessed, totalSpaces, totalTabs, specialCharCounts);
    }

    
}

class Content {

    private int totalLines;
    private int totalSpaces;
    private int totalTabs;
    private Map<Character, Integer> specialCharCounts;

    public Content(int totalLines, int totalSpaces, int totalTabs, Map<Character, Integer> specialCharCounts) {
        this.totalLines = totalLines;
        this.totalSpaces = totalSpaces;
        this.totalTabs = totalTabs;
        this.specialCharCounts = specialCharCounts;
    }

    public int getTotalLines() {
        return this.totalLines;
    }

    public int getTotalSpaces() {
        return this.totalSpaces;
    }

    public int getTotalTabs() {
        return this.totalTabs;
    }

    public Map<Character, Integer> getSpecialCharCounts() {
        return this.specialCharCounts;
    }

    public void setTotalLines(int totalLines) {
        this.totalLines = totalLines;
    }

    public void setTotalSpaces(int totalSpaces) {
        this.totalSpaces = totalSpaces;
    }

    public void setTotalTabs(int totalTabs) {
        this.totalTabs = totalTabs;
    }

    public void setSpecialCharCounts(Map<Character, Integer> specialCharCounts) {
        this.specialCharCounts = specialCharCounts;
    }
}