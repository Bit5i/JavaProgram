import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ContentProcessor {

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: java ContentProcessor <file_path>");
            return;
        }

        String filePath = args[0];
        Content content = new Content();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            Thread[] threads = new Thread[Runtime.getRuntime().availableProcessors()];
            int i = 0;
            while ((line = reader.readLine()) != null) {
                content.setTotalLines(content.getTotalLines() + 1);
                threads[i] = new Thread(new LineProcessor(line, content));
                threads[i].start();
                i = (i + 1) % threads.length;
            }

            //Thread.sleep(100); // Give some time for threads to finish before printing results

            for (Thread thread : threads) {
                if (thread != null) {
                    thread.join();
                }
            }

            reader.close();

            // Print results
            System.out.println("Total number of lines = " + content.getTotalLines());
            System.out.println("Total number of spaces = " + content.getTotalSpaces());
            System.out.println("Total number of tabs = " + content.getTotalTabs());
            for (Map.Entry<Character, Integer> entry : content.getSpecialCharCounts().entrySet()) {
                System.out.println("Total number of " + entry.getKey() + " character = " + entry.getValue());
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }
}

class Content {

    private int totalLines;
    private int totalSpaces;
    private int totalTabs;
    private Map<Character, Integer> specialCharCounts;

    public Content() {
        this.totalLines = 0;
        this.totalSpaces = 0;
        this.totalTabs = 0;
        this.specialCharCounts = new HashMap<>();
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

class LineProcessor implements Runnable {
    private final String line;
    private final Content content;

    public LineProcessor(String line, Content content) {
        this.line = line;
        this.content = content;
    }

    @Override
    public void run() {
        char[] chars = line.toCharArray();
        int lineSpaces = 0;
        int lineTabs = 0;

        for (char c : chars) {
            if (c == ' ') {
                lineSpaces++;
            } else if (c == '\t') {
                lineTabs++;
            } else if (isSpecialCharacter(c)) {
                Map<Character, Integer> specialCharCounts = content.getSpecialCharCounts();
                synchronized (specialCharCounts) {
                    specialCharCounts.put(c, specialCharCounts.getOrDefault(c, 0) + 1);
                    content.setSpecialCharCounts(specialCharCounts);
                }
            }
        }
        
        synchronized (content) {
            content.setTotalSpaces(lineSpaces + content.getTotalSpaces());
            content.setTotalTabs(lineTabs + content.getTotalTabs());
        }
      
    }

    private boolean isSpecialCharacter(char c) {
        return c == '$' || c == '&' || c == '@' || c == '%';
    }
}