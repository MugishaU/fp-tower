package exercises.valfunction

import exercises.valfunction.ValueFunctionExercises._
import org.scalatest.funsuite.AnyFunSuite
import org.scalatestplus.scalacheck.ScalaCheckDrivenPropertyChecks

class ValueFunctionExercisesTest extends AnyFunSuite with ScalaCheckDrivenPropertyChecks {

  /////////////////////////////////////////////////////
  // Exercise 1: String API with higher-order functions
  /////////////////////////////////////////////////////

  // replace `ignore` by `test` to enable the test
  test("selectDigits examples") {
    assert(selectDigits("hello4world-80") == "480")
    assert(selectDigits("welcome") == "")
  }

  // replace `ignore` by `test` to enable the test
  test("selectDigits length is smaller") {
    forAll { (text: String) =>
      assert(selectDigits(text).length <= text.length)
    }
  }

  test("selectDigits outputs only digits") {
    forAll { (text: String) =>
      selectDigits(text).foreach(char => assert(char.isDigit))
    }
  }

  test("secret length is equal") {
    forAll { (text: String) =>
      assert(secret(text).length == text.length)
    }
  }

  test("secret returns only asterisks") {
    forAll { (text: String) =>
      secret(text).foreach(char => assert(char == '*'))
    }
  }

  //Idempotency means calling a function multiple times is the same as calling it once
  test("secret idempotency") {
    forAll { (text: String) =>
      val once = secret(text)
      val twice = secret(secret(text))
      assert(once == twice)
    }
  }

  ///////////////////////
  // Exercise 2: Point
  ///////////////////////

}
