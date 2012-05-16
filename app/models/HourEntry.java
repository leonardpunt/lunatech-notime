package models;

import java.util.List;
import java.util.Set;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.ManyToOne;
import javax.persistence.NoResultException;
import javax.persistence.NonUniqueResultException;
import javax.persistence.Query;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Join;
import javax.persistence.criteria.Root;
import javax.validation.constraints.Min;

import org.hibernate.annotations.Type;
import org.joda.time.LocalDate;

import play.data.validation.Constraints.Max;
import play.data.validation.Constraints.Required;
import play.db.jpa.JPA;
import beans.HourEntryForm;
import beans.HourEntryInWeek;
import datastructures.TotalsAssignment;
import datastructures.TotalsDay;

@Entity
public class HourEntry {

	@Id
	@GeneratedValue
	public Long id;

	@ManyToOne
	public ProjectAssignment assignment;

	@Required
	@Type(type = "org.joda.time.contrib.hibernate.PersistentLocalDate")
	public LocalDate date;

	@Required
	@Min(0)
	@Max(23)
	public Integer hours;

	@Required
	@Min(0)
	@Max(59)
	public Integer minutes;

	@ManyToMany
	@JoinTable(name = "hourentry_tag", joinColumns = @JoinColumn(name = "hourentry_id"), inverseJoinColumns = @JoinColumn(name = "tag_id"))
	public List<Tag> tags;

	public boolean billable;

	@Min(0)
	public Integer rate;

	public HourEntry() {
	}

	public HourEntry(HourEntryForm form) {
		assignment = form.assignment;
		date = form.date;
		hours = form.hours;
		minutes = form.minutes;
		tags = form.getTags();

		// If update and rate equals to null, get saved values for rate and billability
		if (form.id != null && form.rate == null) {
			HourEntry entry = findById(form.id);
			billable = entry.billable;
			rate = entry.rate;
		}
		else {
			billable = form.billable;
			rate = form.rate;
		}
	}

	public HourEntry(HourEntryInWeek form) {
		assignment = form.assignment;
		date = form.date;
		hours = form.hours;
		minutes = form.minutes;
		tags = form.getTags();

		// If update, get saved values for rate and billability
		if (form.id != null) {
			HourEntry entry = findById(form.id);
			billable = entry.billable;
			rate = entry.rate;
		}
	}

	/**
	 * Inserts this hour entry
	 * 
	 */
	public void save() {
		billable = true;
		rate = 100;
		JPA.em().persist(this);
	}

	/**
	 * Updates this hour entry
	 * 
	 * @param entryId
	 *            The id of the hour entry that is going to be updated
	 */
	public void update(Long entryId) {
		id = entryId;
		JPA.em().merge(this);
	}

	/**
	 * Deletes this hour entry
	 */
	public void delete() {
		JPA.em().remove(this);
	}

	/**
	 * Find a hour entry by id
	 * 
	 * @param hourEntryId
	 *            The id of the hour entry to be searched for
	 * @return A {@link HourEntry}
	 */
	public static HourEntry findById(Long hourEntryId) {
		return JPA.em().find(HourEntry.class, hourEntryId);
	}

	/**
	 * Find all hour entries
	 * 
	 * @return A List of {@link HourEntry}s
	 */
	public static List<HourEntry> findAll() {
		CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
		CriteriaQuery<HourEntry> query = cb.createQuery(HourEntry.class);
		query.from(HourEntry.class);
		return JPA.em().createQuery(query).getResultList();
	}

	/**
	 * Find all hour entries for a user project assignment
	 * 
	 * @param assignment
	 *            The @{link ProjectAssignment} which entries are to be searched
	 *            for
	 * @return A List of {@link HourEntry}s
	 */
	public static List<HourEntry> findAllForAssignment(
			final ProjectAssignment assignment) {
		CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
		CriteriaQuery<HourEntry> query = cb.createQuery(HourEntry.class);
		Root<HourEntry> entry = query.from(HourEntry.class);
		query.where(cb.equal(entry.get(HourEntry_.assignment), assignment));
		query.orderBy(cb.desc(entry.get(HourEntry_.id)));
		return JPA.em().createQuery(query).getResultList();
	}

	/**
	 * Find all hour entries for a user between two dates
	 * 
	 * @param user
	 *            The user which entries are to be searched for
	 * @param beginDate
	 *            The date from which entries are to be searched for
	 * @param endDate
	 *            The date till which entries are to be searched for
	 * @return A List of {@link HourEntry}s
	 */
	public static List<HourEntry> findAllForUserBetween(final User user,
			final LocalDate beginDate, final LocalDate endDate) {
		CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
		CriteriaQuery<HourEntry> query = cb.createQuery(HourEntry.class);
		Root<HourEntry> entry = query.from(HourEntry.class);

		Join<HourEntry, ProjectAssignment> assignment = entry
				.join(HourEntry_.assignment);

		query.where(cb.equal(assignment.get(ProjectAssignment_.user), user),
				cb.between(entry.get(HourEntry_.date), beginDate, endDate));
		query.orderBy(cb.desc(entry.get(HourEntry_.id)));
		return JPA.em().createQuery(query).getResultList();
	}

	/**
	 * Find all hour entries for users between two dates
	 * 
	 * @param users
	 *            The users which entries are to be searched for
	 * @param beginDate
	 *            The date from which entries are to be searched for
	 * @param endDate
	 *            The date till which entries are to be searched for
	 * @return A List of {@link HourEntry}s
	 */
	public static List<HourEntry> findAllForUsersBetween(final Set<User> users,
			final LocalDate beginDate, final LocalDate endDate) {
		CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
		CriteriaQuery<HourEntry> query = cb.createQuery(HourEntry.class);
		Root<HourEntry> entry = query.from(HourEntry.class);

		Join<HourEntry, ProjectAssignment> assignment = entry
				.join(HourEntry_.assignment);

		query.where(cb.between(entry.get(HourEntry_.date), beginDate, endDate),
				assignment.get(ProjectAssignment_.user).in(users));
		query.orderBy(cb.desc(entry.get(HourEntry_.id)));
		return JPA.em().createQuery(query).getResultList();
	}

	/**
	 * Find all hour entries for a user for a day
	 * 
	 * @param user
	 *            The user which entries are to be searched for
	 * @param day
	 *            The date on which entries are to be searched for
	 * @return A List of {@link HourEntry}s
	 */
	public static List<HourEntry> findAllForUserForDay(final User user,
			final LocalDate day) {
		CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
		CriteriaQuery<HourEntry> query = cb.createQuery(HourEntry.class);
		Root<HourEntry> entry = query.from(HourEntry.class);

		Join<HourEntry, ProjectAssignment> assignment = entry
				.join(HourEntry_.assignment);

		query.where(cb.equal(assignment.get(ProjectAssignment_.user), user),
				cb.equal(entry.get(HourEntry_.date), day));
		query.orderBy(cb.desc(entry.get(HourEntry_.id)));
		return JPA.em().createQuery(query).getResultList();
	}

	/**
	 * Find all hour entries for projects between two dates
	 * 
	 * @param projects
	 *            The projects which entries are to be searched for
	 * @param beginDate
	 *            The date from which entries are to be searched for
	 * @param endDate
	 *            The date till which entries are to be searched for
	 * @return A List of {@link HourEntry}s
	 */
	public static List<HourEntry> findAllForProjectsBetween(
			Set<Project> projects, LocalDate beginDate, LocalDate endDate) {
		CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
		CriteriaQuery<HourEntry> query = cb.createQuery(HourEntry.class);
		Root<HourEntry> entry = query.from(HourEntry.class);

		Join<HourEntry, ProjectAssignment> assignment = entry
				.join(HourEntry_.assignment);

		query.where(cb.between(entry.get(HourEntry_.date), beginDate, endDate),
				assignment.get(ProjectAssignment_.project).in(projects));
		return JPA.em().createQuery(query).getResultList();
	}

	/**
	 * Calculates the totals of the hour entries for a user, per assignment,
	 * between two dates. Note that the amount of minutes can be more than 60
	 * 
	 * @param user
	 *            The user which entries are to be summed
	 * @param beginDate
	 *            The date from which entries are to be searched for
	 * @param endDate
	 *            The date till which entries are to be searched for
	 * @return A List of {@link TotalsAssignment} objects
	 */
	public static List<TotalsAssignment> findTotalsForUserPerAssignmentBetween(
			final User user, LocalDate beginDate, LocalDate endDate) {
		CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
		CriteriaQuery<TotalsAssignment> query = cb
				.createQuery(TotalsAssignment.class);
		Root<HourEntry> entry = query.from(HourEntry.class);
		query.select(cb.construct(TotalsAssignment.class,
				entry.get(HourEntry_.assignment),
				cb.sumAsLong(entry.get(HourEntry_.hours)),
				cb.sumAsLong(entry.get(HourEntry_.minutes))));

		Join<HourEntry, ProjectAssignment> assignment = entry
				.join(HourEntry_.assignment);

		query.where(cb.equal(assignment.get(ProjectAssignment_.user), user),
				cb.between(entry.get(HourEntry_.date), beginDate, endDate));
		query.groupBy(entry.get(HourEntry_.assignment));
		return JPA.em().createQuery(query).getResultList();
	}

	/**
	 * Calculates the totals of the hour entries for a user, per day, between
	 * two dates. Note that the amount of minutes can be more than 60
	 * 
	 * @param user
	 *            The user which entries are to be summed
	 * @param beginDate
	 *            The date from which entries are to be searched for
	 * @param endDate
	 *            The date till which entries are to be searched for
	 * @return A List of {@link TotalsDay} objects
	 */
	public static List<TotalsDay> findTotalsForUserPerDayBetween(
			final User user, final LocalDate beginDate, final LocalDate endDate) {
		CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
		CriteriaQuery<TotalsDay> query = cb.createQuery(TotalsDay.class);
		Root<HourEntry> entry = query.from(HourEntry.class);
		query.select(cb.construct(TotalsDay.class, entry.get(HourEntry_.date),
				cb.sumAsLong(entry.get(HourEntry_.hours)),
				cb.sumAsLong(entry.get(HourEntry_.minutes))));

		Join<HourEntry, ProjectAssignment> assignment = entry
				.join(HourEntry_.assignment);

		query.where(cb.equal(assignment.get(ProjectAssignment_.user), user),
				cb.between(entry.get(HourEntry_.date), beginDate, endDate));
		query.groupBy(entry.get(HourEntry_.date));
		query.orderBy(cb.asc(entry.get(HourEntry_.date)));
		return JPA.em().createQuery(query).getResultList();
	}

	/**
	 * Calculates the totals of the hour entries for a user, for a day. Note
	 * that the amount of minutes can be more than 60
	 * 
	 * @param user
	 *            The user which entries are to be summed
	 * @param day
	 *            The day for which the entries are to be summed
	 * @return A {@link TotalsDay} with the totals
	 */
	public static TotalsDay findTotalsForUserForDay(final User user,
			final LocalDate day) {
		CriteriaBuilder cb = JPA.em().getCriteriaBuilder();
		CriteriaQuery<TotalsDay> query = cb.createQuery(TotalsDay.class);
		Root<HourEntry> entry = query.from(HourEntry.class);
		query.select(cb.construct(TotalsDay.class, entry.get(HourEntry_.date),
				cb.sumAsLong(entry.get(HourEntry_.hours)),
				cb.sumAsLong(entry.get(HourEntry_.minutes))));

		Join<HourEntry, ProjectAssignment> assignment = entry
				.join(HourEntry_.assignment);

		query.where(cb.equal(assignment.get(ProjectAssignment_.user), user),
				cb.equal(entry.get(HourEntry_.date), day));
		query.groupBy(entry.get(HourEntry_.date));
		Query typedQuery = JPA.em().createQuery(query);
		try {
			return (TotalsDay) typedQuery.getSingleResult();
		} catch (NoResultException nre) {
			return new TotalsDay(day, 0L, 0L);
		} catch (NonUniqueResultException nure) {
			return new TotalsDay(day, 0L, 0L);
		}
	}

	/**
	 * Creates a String of the related tags, delimited by a semicolon
	 * 
	 * @return String of related tags, delimited by a semicolon
	 */
	public String enteredTagsString() {
		return Tag.tagsToString(tags);
	}

}
