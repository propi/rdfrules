import com.github.propi.rdfrules.algorithm.amie.Amie
import com.github.propi.rdfrules.data.{Dataset, RdfSource}
import com.github.propi.rdfrules.index.Index
import org.scalatest.{FlatSpec, Inside, Matchers}
import com.github.propi.rdfrules.data.formats.Tsv._

/**
  * Created by Vaclav Zeman on 18. 4. 2018.
  */
class RulesetSpec extends FlatSpec with Matchers with Inside {

  private lazy val dataset1 = Dataset[RdfSource.Tsv.type](GraphSpec.dataYago)

  "Index" should "mine directly from index" in {
    Index(dataset1).mine(Amie()).size shouldBe 116
  }

  "Dataset" should "mine directly from dataset" in {
    dataset1.mine(Amie()).size shouldBe 116
  }



}
