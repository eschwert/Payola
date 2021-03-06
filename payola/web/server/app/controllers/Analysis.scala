package controllers

import helpers.Secured
import cz.payola.domain.entities.User
import cz.payola.web.shared.Payola

object Analysis extends PayolaController with Secured
{
    def detail(id: String) = maybeAuthenticated { user: Option[User] =>
        Payola.model.analysisModel.getById(id).map(a => Ok(views.html.analysis.detail(user, a))).getOrElse {
            NotFound(views.html.errors.err404("The analysis does not exist."))
        }
    }

    def create = authenticated { user =>
        Ok(views.html.analysis.create(user))
    }

    def edit(id: String) = authenticated { user =>
        Ok(views.html.analysis.edit(user, id))
    }

    def delete(id: String) = authenticatedWithRequest { (user, request) =>
        user.ownedAnalyses.find(_.id == id).map(Payola.model.analysisModel.remove(_))
            .getOrElse(NotFound("Analysis not found."))

        Redirect(routes.Analysis.list())
    }

    def list(page: Int = 1) = authenticated { user: User =>
        Ok(views.html.analysis.list(Some(user), user.ownedAnalyses, page))
    }

    def listAccessible(page: Int = 1) = maybeAuthenticated { user: Option[User] =>
        Ok(views.html.analysis.list(user, Payola.model.analysisModel.getAccessibleToUser(user), page, Some("Accessible analyses")))
    }

    def listAccessibleByOwner(ownerId: String, page: Int = 1) = maybeAuthenticated { user: Option[User] =>
        val owner = Payola.model.userModel.getById(ownerId)
        val analyses = if (owner.isDefined) {
            Payola.model.analysisModel.getAccessibleToUserByOwner(user, owner.get)
        }else{
            List()
        }
        Ok(views.html.analysis.list(user, analyses, page))
    }
}
