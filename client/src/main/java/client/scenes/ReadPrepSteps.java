package client.scenes;

import commons.PreparationStep;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReadPrepSteps {
    private static final Pattern STEP_PATTERN = Pattern.compile("^(\\d+)\\)$");
    private List<PreparationStep> steps;

    public static class PrepStepsParseException extends Exception {
        public PrepStepsParseException(String message) { super(message); }
    }

    public List<PreparationStep> readAndParse(String text) throws PrepStepsParseException {
        if (text == null || text.trim().isEmpty()) throw new PrepStepsParseException("Preparation steps cannot be empty");
        steps = parse(text);
        return steps;
    }

    private List<PreparationStep> parse(String text) throws PrepStepsParseException {
        String[] lines = text.split("\n", -1);
        List<PreparationStep> result = new ArrayList<>();
        int i = 0;
        int expected = 1;
        while (i < lines.length) {
            while (i < lines.length && lines[i].trim().isEmpty()) i++;
            if (i >= lines.length) break;
            String header = lines[i].trim();
            Matcher m = STEP_PATTERN.matcher(header);
            checkInvalidStep(m, i, header);
            int num = Integer.parseInt(m.group(1));
            num = checkStep(num, expected);
            i++;
            if (i >= lines.length) throw new PrepStepsParseException(String.format("Step %d is missing instruction text.", num));
            String instruction = lines[i].trim();
            checkInstructionText(i, num, lines, instruction);
            i++;
            checkBlankLine(lines, i, num);
            PreparationStep step = new PreparationStep(instruction);
            step.setStepOrder(num);
            result.add(step);
            expected++;
        }
        if (result.isEmpty()) throw new PrepStepsParseException("No valid preparation steps found");
        return result;
    }

    private void checkInvalidStep(Matcher m, int i, String header) throws PrepStepsParseException {
        if (!m.matches()) throw new PrepStepsParseException(String.format("Invalid step header at line %d: '%s'. Expected format: '<number>)'", i + 1, header));
    }

    private int checkStep(int num, int expected) throws PrepStepsParseException {
        if (num != expected) {
            if (num < expected) throw new PrepStepsParseException(String.format("Step %d is out of sequence. Expected step %d.", num, expected));
            else throw new PrepStepsParseException(String.format("Step %d is missing. Expected step %d.", expected, expected));
        }
        return num;
    }

    private void checkInstructionText(int i, int num, String[] lines, String instruction) throws PrepStepsParseException {
        if (i >= lines.length) throw new PrepStepsParseException(String.format("Step %d is missing instruction text.", num));
        if (lines[i].trim().isEmpty()) throw new PrepStepsParseException(String.format("Step %d has no instruction text immediately following the step number.", num));
        if (instruction.isEmpty()) throw new PrepStepsParseException(String.format("Step %d has empty instruction text.", num));
    }

    private void checkBlankLine(String[] lines, int i, int num) throws PrepStepsParseException {
        if (i < lines.length) {
            if (!lines[i].trim().isEmpty()) throw new PrepStepsParseException(String.format("Step %d must be followed by exactly one blank line.", num));
            i++;
            if (i < lines.length && lines[i].trim().isEmpty()) {
                int peek = i;
                while (peek < lines.length && lines[peek].trim().isEmpty()) peek++;
                if (peek < lines.length) throw new PrepStepsParseException(String.format("Extra blank lines found after step %d. Only one blank line is allowed between steps.", num));
            }
        }
    }

    public List<PreparationStep> getPreparationSteps() { return steps; }
    public void clear() { if (steps != null) steps.clear(); steps = null; }
    public boolean hasSteps() { return steps != null && !steps.isEmpty(); }
    public int getStepCount() { return steps != null ? steps.size() : 0; }
    public static boolean validate(String text) throws PrepStepsParseException { new ReadPrepSteps().readAndParse(text); return true; }
}