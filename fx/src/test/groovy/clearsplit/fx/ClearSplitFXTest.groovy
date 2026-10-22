package clearsplit.fx

import javafx.application.Platform
import javafx.embed.swing.SwingFXUtils
import org.apache.groovy.groovyfx.GroovyFX
import org.apache.groovy.groovyfx.SceneGraphBuilder
import org.junit.jupiter.api.Test

class ClearSplitFXTest {
    @Test
    void 'a group can be started, charged and settled through the UI'() {
        Platform.implicitExit = false
        ClearSplitFX app
        GroovyFX.start { stage -> app = new ClearSplitFX(fx: delegate as SceneGraphBuilder); app.show() }
        def fx = app.fx
        fx.fx {
            membersInput.text = 'Alice\nBob\nCarol'
            app.start()
            description.text = 'Dinner'
            amount.text = '90'
            addExpense.fire()
            // Carol pays for a taxi Alice didn't take
            description.text = 'Taxi'
            payer.value = 'Carol'
            amount.text = '10'
            participants.children[0].selected = false
            addExpense.fire()
            from.value = 'Bob'; to.value = 'Alice'; paid.text = '20'
            recordPayment.fire()
        }
        assert app.group.balances() == [4000, -1500, -2500] as int[]
        assert fx.status.text == 'Payment recorded'
        assert fx.history.items.size() == 3
        assert fx.table.children*.text.contains('$40.00')
        snapshot(fx, 'balances')
    }

    static void snapshot(SceneGraphBuilder fx, String name) {
        def dir = new File(System.getProperty('snapshotDir', 'build/snapshots')).tap { mkdirs() }
        fx.fx {
            def image = fx.primaryStage.scene.snapshot(null)
            javax.imageio.ImageIO.write(SwingFXUtils.fromFXImage(image, null), 'png', new File(dir, "${name}.png"))
        }
    }
}
