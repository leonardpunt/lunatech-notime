package beans;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import models.HourEntry;
import models.ProjectAssignment;
import models.Tag;

import org.joda.time.LocalDate;

import utils.DateUtil;

/**
 * Used to bind the submitted hourentries form the weekoverview.
 */
public class HourEntryInWeek {

	public Long id;

	public ProjectAssignment assignment;

	public LocalDate date;

	public Integer hours;

	public Integer minutes;

	public String tagsString;

	private final List<String> errors;

	private boolean valid;

	public HourEntryInWeek() {
		errors = new LinkedList<String>();
	}

	public HourEntryInWeek(HourEntry entry) {
		id = entry.id;
		assignment = entry.assignment;
		date = entry.date;
		hours = entry.hours;
		minutes = entry.minutes;
		tagsString = entry.enteredTagsString();
		errors = new LinkedList<String>();
	}

	public List<Tag> getTags() {
		if (!tagsString.isEmpty()) {
			List<Tag> tags = new LinkedList<Tag>();
			String splittedTags[] = tagsString.split(";");
			for (int i = 0; i < splittedTags.length; i++)
				tags.add(Tag.findOrCreate(splittedTags[i]));
			return tags;
		}
		return Collections.emptyList();
	}

	/**
	 * Checks if hours or minutes has null as value. if true it replaces it with
	 * zero
	 */
	public void setHoursAndMinutesFromNullToZero() {
		if (hours == null)
			hours = 0;
		if (minutes == null)
			minutes = 0;
	}

	/**
	 * A entry is valid if it has a date, assignment, hours and minutes. And the
	 * hours are less than 24 and minutes less than 60. This method does all
	 * validations. It doesn't stop at the first error.
	 * 
	 * @return true is the entry is valid
	 */
	public boolean isValid() {
		valid = true;
		validate();
		return valid;
	}

	private void validate() {
		hasDate();
		hasAssignment();
		hasHours();
		hasMinutes();
		hasLessThan24Hours();
		hasLessThan60Minutes();
		isDateInAssignmentRange();
	}

	public boolean hasDate() {
		if (date == null) {
			errors.add("Invalid input for date");
			valid = false;
			return false;
		}
		return true;
	}

	public boolean hasAssignment() {
		if (assignment == null || assignment.id == null) {
			errors.add("Invalid input for assignment");
			valid = false;
			return false;
		}
		assignment = ProjectAssignment.findById(assignment.id);
		hasActiveAssignment();
		return true;
	}

	private boolean hasActiveAssignment() {
		if (!assignment.active) {
			errors.add("Assignment is not active");
			valid = false;
			return false;
		}
		return true;
	}

	public boolean isDateInAssignmentRange() {
		if (!ProjectAssignment.isDateInAssignmentRange(date, assignment.id)) {
			errors.add("Invalid input, you are assigned to "
					+ assignment.project.code + " untill "
					+ DateUtil.formatDate(assignment.endDate, "E d-M"));
			valid = false;
			return false;
		}
		return true;
	}

	public boolean hasNot0Hours() {
		return hours != null && hours != 0;
	}

	public boolean hasNot0Minutes() {
		return minutes != null && minutes != 0;
	}

	public boolean hasHours() {
		if (hours == null) {
			errors.add("Invalid input for hours");
			valid = false;
			return false;
		}
		return true;
	}

	public boolean hasMinutes() {
		if (minutes == null) {
			errors.add("Invalid input for minutes");
			valid = false;
			return false;
		}
		return true;
	}

	public boolean hasLessThan24Hours() {
		if (!hasHours() || hours < 0 || hours >= 24) {
			errors.add("Hours can't be less than 0 or more than 23");
			valid = false;
			return false;
		}
		return true;
	}

	public boolean hasLessThan60Minutes() {
		if (!hasMinutes() || minutes < 0 || minutes >= 60) {
			errors.add("Minutes can't be less than 0 or more than 59");
			valid = false;
			return false;
		}
		return true;
	}

	public List<String> getErrors() {
		return errors;
	}

	public boolean hasErrors() {
		return !errors.isEmpty();
	}

	public HourEntry toHourEntry() {
		return new HourEntry(this);
	}

}
