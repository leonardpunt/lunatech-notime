package controllers;

import java.util.Collections;
import java.util.List;
import models.Project;
import models.User;
import play.data.Form;
import play.data.validation.ValidationError;
import play.db.jpa.Transactional;
import play.i18n.Messages;
import play.mvc.Controller;
import play.mvc.Result;
import play.mvc.Security;
import views.html.project.createProject;
import views.html.project.editProject;
import views.html.project.project;
import views.html.project.projects;
import be.objectify.deadbolt.actions.And;
import be.objectify.deadbolt.actions.Restrictions;

@Security.Authenticated(Secured.class)
@Restrictions({ @And("admin"), @And("customerManager") })
public class Projects extends Controller {

	@Transactional(readOnly = true)
	@Security.Authenticated(Secured.class)
	@Restrictions({ @And("admin"), @And("customerManager"), @And("projectManager") })
	public static Result all() {
		List<Project> projectsList = Collections.emptyList();
		final User user = Application.getCurrentUser();

		if (user.hasAdminRole()) {
			projectsList = Project.findAll();
		} else {
			if (user.hasCustomerManagerRole())
				projectsList = Project.findAllForCustomerManager(user);
			else 
				projectsList = Project.findAllForProjectManager(user);
		}
		return ok(projects.render(projectsList));
	}

	@Transactional(readOnly = true)
	@Security.Authenticated(Secured.class)
	@Restrictions({ @And("admin"), @And("customerManager"),	@And("projectManager") })
	public static Result read(Long projectId) {
		return ok(project.render(Project.findById(projectId)));
	}

	@Transactional(readOnly = true)
	public static Result add() {
		Form<Project> newForm = form(Project.class);
		return ok(createProject.render(newForm));
	}

	@Transactional
	public static Result create() {
		Form<Project> filledForm = form(Project.class).bindFromRequest();

		if (filledForm.hasErrors())
			return badRequest(createProject.render(filledForm));

		final Project project = filledForm.get();
		final User user = Application.getCurrentUser();

		// Check if customer manager is allowed to do this
		if (!user.hasAdminRole() && user.hasCustomerManagerRole()) {
			if (!project.customer.customerManagers.contains(user)) {
				filledForm.reject(new ValidationError("customer.id", Messages
						.get("customer.notCustomerManager"), null));
				return badRequest(createProject.render(filledForm));
			}
		}

		project.save();
		return redirect(routes.Projects.all());
	}

	@Transactional(readOnly = true)
	public static Result edit(Long id) {
		final User user = Application.getCurrentUser();
		final Project project = Project.findById(id);

		// Check if customer manager is allowed to do this
		if (!user.hasAdminRole() && user.hasCustomerManagerRole()) {
			if (!project.customer.customerManagers.contains(user)) {
				flash("error", Messages.get("project.notCustomerManager"));
				return redirect(routes.Projects.all());
			}
		}

		Form<Project> filledForm = form(Project.class).fill(project);
		return ok(editProject.render(id, filledForm));
	}

	@Transactional
	public static Result update(Long id) {
		Form<Project> filledForm = form(Project.class).bindFromRequest();

		if (filledForm.hasErrors())
			return badRequest(editProject.render(id, filledForm));

		final Project project = filledForm.get();
		final User user = Application.getCurrentUser();

		// Check if customer manager is allowed to do this
		if (!user.hasAdminRole() && user.hasCustomerManagerRole()) {
			if (!project.customer.customerManagers.contains(user)) {
				filledForm.reject(new ValidationError("customer.id", Messages
						.get("customer.notCustomerManager"), null));
				return badRequest(editProject.render(id, filledForm));
			}
		}

		project.update(id);
		return redirect(routes.Projects.all());
	}

	@Transactional
	public static Result delete(Long id) {
		final User user = Application.getCurrentUser();
		final Project project = Project.findById(id);

		// Check if customer manager is allowed to do this
		if (!user.hasAdminRole() && user.hasCustomerManagerRole()) {
			if (!project.customer.customerManagers.contains(user)) {
				flash("error", Messages.get("project.notCustomerManager"));
				return redirect(routes.Projects.all());
			}
		}

		if (!Project.findById(id).delete()) {
			flash("error", Messages.get("project.notDeletable"));
		}

		return redirect(routes.Projects.all());
	}

}
