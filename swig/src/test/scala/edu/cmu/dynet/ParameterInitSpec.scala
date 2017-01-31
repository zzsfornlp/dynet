package edu.cmu.dynet

import org.scalatest._
import edu.cmu.dynet._
import edu.cmu.dynet.dynet_swig._

class ParameterInitSpec extends FlatSpec with Matchers {

  import DynetScalaHelpers._
  myInitialize()

  "ParameterInitConst" should "set constant values" in {
    val model = new Model
    val p_W = model.add_parameters(dim(10))

    val init = new ParameterInitConst(10.0f)
    init.initialize_params(p_W.values)

    // All values should be initialized to 10.0f
    p_W.values.toSeq.foreach(x => x shouldBe 10.0f)
  }

  "ParameterInitNormal" should "initialize things normally" in {
    val model = new Model
    val p_W = model.add_parameters(dim(10000))

    // mean 10, variance 4
    val init = new ParameterInitNormal(10, 4)
    init.initialize_params(p_W.values)

    val values = p_W.values.toSeq

    // Really weak bounds on the sample mean and variance, basically just a sanity check.
    val mean = values.sum / 10000
    mean > 9  shouldBe true
    mean < 11 shouldBe true

    val s2 = values.map(v => scala.math.pow(v - mean, 2)).sum / 9999f
    s2 > 3 shouldBe true
    s2 < 5 shouldBe true
  }

  "ParameterInitUniform" should "initialize things uniformly" in {
    val model = new Model
    val p_W = model.add_parameters(dim(10000))

    // uniform from 12 to 17
    val init = new ParameterInitUniform(12f, 17f)
    init.initialize_params(p_W.values)

    val values = p_W.values.toSeq

    values.max <= 17 shouldBe true
    values.min >= 12 shouldBe true

    // Really weak bounds on the sample mean and variance, basically just a sanity check.
    val mean = values.sum / 10000
    mean > 14 shouldBe true
    mean < 15 shouldBe true

    // Theoretical variance is 25/12
    val s2 = values.map(v => scala.math.pow(v - mean, 2)).sum / 9999f
    s2 > 20 / 12f shouldBe true
    s2 < 30 / 12f shouldBe true

  }

  "ParameterInitIdentity" should "initialize to the identity matrix" in {
    val model = new Model
    val p_W = model.add_parameters(dim(100, 100))

    val init = new ParameterInitIdentity()
    init.initialize_params(p_W.values)

    for {
      i <- 0 until 100
      j <- 0 until 100
      z = if (i == j) 1f else 0f
    } {
      TensorTools.AccessElement(p_W.values, dim(i, j)) shouldBe z
    }
  }

  "ParameterInitFromVector" should "initialize from a vector" in {
    val model = new Model
    val p_W = model.add_parameters(dim(1000))

    val valuesIn = (1 to 1000).map(x => math.sin(x).toFloat)
    val vector = new FloatVector(valuesIn)
    val init = new ParameterInitFromVector(vector)

    init.initialize_params(p_W.values)

    val valuesOut = p_W.values.toSeq

    valuesIn.zip(valuesOut).foreach {
      case (vi, vo) => vi shouldBe vo
    }
  }

  "ParameterInitGlorot" should "initialize using the correct distribution" in {
    val model = new Model
    val p_W = model.add_parameters(dim(20, 5))

    val init = new ParameterInitGlorot()

    init.initialize_params(p_W.values)

    // should be uniform [-s, s] where s = sqrt(6) / sqrt(25) = sqrt(6) / 5
    val s = math.sqrt(6) / 5 // ~ 0.4899

    val values = p_W.values.toSeq

    // within the bounds
    values.min > -s shouldBe true
    values.max <  s shouldBe true

    // reasonable looking mean
    val mean = values.sum / 100
    mean <  0.2 shouldBe true
    mean > -0.2 shouldBe true

    // but some dispersion (values in lowest / highest quartiles)
    values.exists(v => v > s  / 2) shouldBe true
    values.exists(v => v < -s / 2) shouldBe true
  }
}