import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

public class EmployeePairAnalyzer {

    static class WorkPeriod {
        int empId;
        int projectId;
        LocalDate dateFrom;
        LocalDate dateTo;

        public WorkPeriod(int empId, int projectId, LocalDate dateFrom, LocalDate dateTo) {
            this.empId = empId;
            this.projectId = projectId;
            this.dateFrom = dateFrom;
            this.dateTo = dateTo;
        }
    }

    static class EmployeePair {
        int emp1;
        int emp2;

        public EmployeePair(int emp1, int emp2) {
            this.emp1 = Math.min(emp1, emp2);
            this.emp2 = Math.max(emp1, emp2);
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            EmployeePair that = (EmployeePair) o;
            return emp1 == that.emp1 && emp2 == that.emp2;
        }

        @Override
        public int hashCode() {
            return Objects.hash(emp1, emp2);
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: java EmployeePairAnalyzer <filename.csv>");
            System.exit(1);
        }

        String fileName = args[0];
        List<WorkPeriod> workPeriods = parseCSV(fileName);

        if (workPeriods.isEmpty()) {
            System.out.println("No valid data found in the file.");
            return;
        }

        Map<EmployeePair, Long> pairDurations = calculatePairDurations(workPeriods);

        if (pairDurations.isEmpty()) {
            System.out.println("No employees worked together on common projects.");
            return;
        }

        EmployeePair longestPair = null;
        long maxDuration = 0;

        for (Map.Entry<EmployeePair, Long> entry : pairDurations.entrySet()) {
            if (entry.getValue() > maxDuration) {
                maxDuration = entry.getValue();
                longestPair = entry.getKey();
            }
        }

        System.out.println(longestPair.emp1 + ", " + longestPair.emp2 + ", " + maxDuration);
    }

    private static List<WorkPeriod> parseCSV(String fileName) {
        List<WorkPeriod> workPeriods = new ArrayList<>();
        DateTimeFormatter[] formatters = {
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("M/d/yyyy"),
            DateTimeFormatter.ofPattern("dd/MM/yyyy")
        };

        try (BufferedReader br = new BufferedReader(new FileReader(fileName))) {
            String line;
            boolean firstLine = true;

            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    // Skip header if it exists (check if first field is not a number)
                    String firstField = line.split(",")[0].trim();
                    try {
                        Integer.parseInt(firstField);
                    } catch (NumberFormatException e) {
                        continue; // Skip header line
                    }
                }

                String[] parts = line.split(",");
                if (parts.length < 4) continue;

                try {
                    int empId = Integer.parseInt(parts[0].trim());
                    int projectId = Integer.parseInt(parts[1].trim());
                    LocalDate dateFrom = parseDate(parts[2].trim(), formatters);
                    LocalDate dateTo;

                    String dateToStr = parts[3].trim();
                    if (dateToStr.equalsIgnoreCase("NULL") || dateToStr.isEmpty()) {
                        dateTo = LocalDate.now();
                    } else {
                        dateTo = parseDate(dateToStr, formatters);
                    }

                    workPeriods.add(new WorkPeriod(empId, projectId, dateFrom, dateTo));
                } catch (Exception e) {
                    System.err.println("Error parsing line: " + line + " - " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading file: " + e.getMessage());
            System.exit(1);
        }

        return workPeriods;
    }

    private static LocalDate parseDate(String dateStr, DateTimeFormatter[] formatters) {
        for (DateTimeFormatter formatter : formatters) {
            try {
                return LocalDate.parse(dateStr, formatter);
            } catch (Exception e) {
                // Try next formatter
            }
        }
        throw new IllegalArgumentException("Unable to parse date: " + dateStr);
    }

    private static Map<EmployeePair, Long> calculatePairDurations(List<WorkPeriod> workPeriods) {
        Map<EmployeePair, Long> pairDurations = new HashMap<>();
        Map<Integer, List<WorkPeriod>> projectMap = new HashMap<>();

        // Group work periods by project
        for (WorkPeriod wp : workPeriods) {
            projectMap.computeIfAbsent(wp.projectId, k -> new ArrayList<>()).add(wp);
        }

        // For each project, find overlapping periods between different employees
        for (List<WorkPeriod> periods : projectMap.values()) {
            for (int i = 0; i < periods.size(); i++) {
                for (int j = i + 1; j < periods.size(); j++) {
                    WorkPeriod wp1 = periods.get(i);
                    WorkPeriod wp2 = periods.get(j);

                    if (wp1.empId == wp2.empId) continue;

                    long overlap = calculateOverlap(wp1.dateFrom, wp1.dateTo, wp2.dateFrom, wp2.dateTo);
                    if (overlap > 0) {
                        EmployeePair pair = new EmployeePair(wp1.empId, wp2.empId);
                        pairDurations.put(pair, pairDurations.getOrDefault(pair, 0L) + overlap);
                    }
                }
            }
        }

        return pairDurations;
    }

    private static long calculateOverlap(LocalDate start1, LocalDate end1, LocalDate start2, LocalDate end2) {
        LocalDate overlapStart = start1.isAfter(start2) ? start1 : start2;
        LocalDate overlapEnd = end1.isBefore(end2) ? end1 : end2;

        if (overlapStart.isAfter(overlapEnd)) {
            return 0;
        }

        return ChronoUnit.DAYS.between(overlapStart, overlapEnd);
    }
}
