
package controllers.interest

import play.api.http.Status._
import play.api.mvc.Result
import utils.ViewTest
import views.html.interest.ReceiveUntaxedInterestView

import scala.concurrent.Future

class ReceiveUntaxedInterestControllerSpec extends ViewTest {

  val view = app.injector.instanceOf[ReceiveUntaxedInterestView]

  lazy val controller = new ReceiveUntaxedInterestController(
    mockMessagesControllerComponents,
    authorisedAction,
    view,
    mockAppConfig
  )

  val taxYear = 2020

  ".show" should {

    "return a result" which {

      s"has an OK($OK) status" in new TestWithAuth {
        val result: Future[Result] = controller.show(taxYear)(fakeRequest)

        status(result) shouldBe OK
      }
    }
  }

  ".submit" should {

    "return a result" which {
      s"has a redirect($SEE_OTHER) status" in new TestWithAuth {
        val result: Future[Result] = controller.submit(taxYear)(fakeRequest)

        status(result) shouldBe SEE_OTHER
      }
    }
  }

}
