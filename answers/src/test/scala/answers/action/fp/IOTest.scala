package answers.action.fp

import org.scalacheck.Gen
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger
import scala.concurrent.ExecutionContext.global
import scala.util.{Failure, Success, Try}

class IOTest extends AnyFunSuite with ScalaCheckDrivenPropertyChecks {

  test("apply is lazy and repeatable") {
    var counter = 0

    val action = IO(counter += 1)
    assert(counter == 0)

    action.unsafeRun()
    assert(counter == 1)

    action.unsafeRun()
    assert(counter == 2)
  }

  test("andThen") {
    var counter = 0

    val first  = IO(counter += 1)
    val second = IO(counter *= 2)

    val action = first.andThen(second)
    assert(counter == 0) // nothing happened before unsafeRun

    action.unsafeRun()
    assert(counter == 2) // first and second were executed in the expected order
  }

  test("map") {
    var counter = 0

    val first  = IO(counter += 1)
    val action = first.map(_ => 1)
    assert(counter == 0) // nothing happened before unsafeRun

    action.unsafeRun()
    assert(counter == 1) // first was executed
  }

  test("flatMap") {
    var counter = 0

    val first  = IO(counter += 1)
    val second = IO(counter *= 2)

    val action = first.flatMap(_ => second)
    assert(counter == 0) // nothing happened before unsafeRun

    action.unsafeRun()
    assert(counter == 2) // first and second were executed
  }

  test("onError success") {
    var counter = 0

    val action = IO { counter += 1; "" }.onError(_ => IO(counter *= 2))
    assert(counter == 0) // nothing happened before unsafeRun

    val result = action.attempt.unsafeRun()
    assert(counter == 1) // first action was executed but not the callback
    assert(result == Success(""))
  }

  test("onError failure") {
    var counter = 0
    val error1  = new Exception("Boom 1")

    val action = IO(throw error1).onError(_ => IO(counter += 1))
    assert(counter == 0) // nothing happened before unsafeRun

    val result = Try(action.unsafeRun())
    assert(counter == 1) // callback was executed
    assert(result == Failure(error1))
  }

  test("onError with two failures") {
    var counter = 0
    val error1  = new Exception("Boom 1")
    val error2  = new Exception("Boom 2")

    val action = IO(throw error1)
      .onError(_ => IO(counter += 1).andThen(IO(throw error2)))
    assert(counter == 0) // nothing happened before unsafeRun

    val result = Try(action.unsafeRun())
    assert(counter == 1)              // callback was executed
    assert(result == Failure(error1)) // callback error was swallowed
  }

  test("handleErrorWith success") {
    var counter = 0

    val first  = IO(counter += 1) andThen IO("A")
    val second = IO(counter *= 2) andThen IO("B")
    val action = first.handleErrorWith(_ => second)
    assert(counter == 0) // nothing happened before unsafeRun

    val result = action.unsafeRun()
    assert(result == "A")
    assert(counter == 1) // only first is executed
  }

  test("handleErrorWith failure") {
    var counter = 0

    val first  = IO(counter += 1) andThen IO.fail[Unit](new Exception("Boom"))
    val second = IO(counter *= 2)
    val action = first.handleErrorWith(_ => second)
    assert(counter == 0) // nothing happened before unsafeRun

    action.unsafeRun()
    assert(counter == 2) // first and second were executed in the expected order
  }

  test("retry, maxAttempt must be greater than 0") {
    forAll(Gen.choose(Int.MinValue, 0)) { (maxAttempt: Int) =>
      val result = IO(1).retry(maxAttempt).attempt.unsafeRun()

      assert(result.isFailure)
    }
  }

  test("retry until action succeeds") {
    forAll(
      Gen.choose(1, 10000),
      Gen.choose(0, 10000)
    ) { (maxAttempt: Int, numberOfError: Int) =>
      var counter = 0
      val action = IO {
        if (counter < numberOfError) {
          counter += 1
          throw new Exception("Boom")
        } else "Hello"
      }

      val result = action.retry(maxAttempt).attempt.unsafeRun()

      if (maxAttempt > numberOfError)
        assert(result == Success("Hello"))
      else {
        assert(result.isFailure)
        assert(result.failed.get.getMessage == "Boom")
      }
    }
  }

  // flaky
  ignore("parZip first faster than second") {
    val counter = new AtomicInteger(0)

    val first  = IO(counter.incrementAndGet())
    val second = IO.sleep(Duration.ofMillis(10)) *> IO(counter.set(5)) *> IO(counter.get())

    val action = first.parZip(second)(global)
    assert(counter.get() == 0)

    assert(action.unsafeRun() == (1, 5))
    assert(counter.get() == 5)
  }

  // flaky
  ignore("parZip second faster than first") {
    val counter = new AtomicInteger(0)

    val first  = IO.sleep(Duration.ofMillis(10)) *> IO(counter.incrementAndGet())
    val second = IO(counter.set(5)) *> IO(counter.get())

    val action = first.parZip(second)(global)
    assert(counter.get() == 0)

    assert(action.unsafeRun() == (6, 5))
    assert(counter.get() == 6)
  }

  test("sequence") {
    var counter = 0

    val action = IO.sequence(
      List(
        IO { counter += 2; counter },
        IO { counter *= 3; counter },
        IO { counter -= 1; counter }
      )
    )
    assert(counter == 0)

    assert(action.unsafeRun() == List(2, 6, 5))
    assert(counter == 5)

  }

  test("traverse") {
    var counter = 0

    val values: List[Int => Int] = List(_ + 2, _ * 3, _ - 1)

    val action = IO.traverse(values)(f => IO { counter = f(counter); counter })
    assert(counter == 0)

    assert(action.unsafeRun() == List(2, 6, 5))
    assert(counter == 5)
  }

}