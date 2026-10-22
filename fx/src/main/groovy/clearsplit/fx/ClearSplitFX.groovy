package clearsplit.fx

import clearsplit.Group
import clearsplit.Settlement
import javafx.application.Platform
import javafx.scene.chart.XYChart
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import org.apache.groovy.groovyfx.GroovyFX
import org.apache.groovy.groovyfx.SceneGraphBuilder

/**
 * ClearSplit as a desktop app, built with groovy-fx's SceneGraphBuilder over the same
 * verified core as the Grails app.
 */
class ClearSplitFX {
    Group group
    SceneGraphBuilder fx

    static void main(String[] args) {
        Platform.implicitExit = true
        GroovyFX.start { stage -> new ClearSplitFX(fx: delegate as SceneGraphBuilder).show() }
    }

    void show() {
        fx.stage(title: 'ClearSplit', width: 560, height: 520, show: true) {
            scene(id: 'setupScene') {
                vbox(spacing: 12, padding: 24) {
                    label('Start a group', style: '-fx-font-size: 18; -fx-font-weight: bold')
                    label('Who is sharing expenses? One name per line, or separated by commas.')
                    textArea(id: 'membersInput', prefRowCount: 5, promptText: 'Alice\nBob\nCarol')
                    label(id: 'setupError', textFill: 'firebrick')
                    button('Start', defaultButton: true, maxWidth: Double.MAX_VALUE, onAction: { start() })
                }
            }
        }
    }

    void start() {
        try {
            group = Group.of(fx.membersInput.text.split(/[,\n]/) as List<String>)
        } catch (IllegalArgumentException e) {
            fx.setupError.text = e.message
            return
        }
        fx.primaryStage.scene = mainScene()
        refresh('')
    }

    def mainScene() {
        def names = group.members()
        def indexOf = { String name -> names.indexOf(name) }
        fx.scene {
            borderPane {
                top {
                    vbox(padding: [12, 16, 4, 16]) {
                        label('ClearSplit', style: '-fx-font-size: 18; -fx-font-weight: bold')
                        label(names.join(' / '), textFill: 'gray')
                    }
                }
                center {
                    tabPane(tabClosingPolicy: 'UNAVAILABLE') {
                        tab('Balances') {
                            vbox(spacing: 8, padding: 12) {
                                barChart(id: 'chart', legendVisible: false, animated: false, prefHeight: 240,
                                    xAxis: categoryAxis(), yAxis: numberAxis(label: 'dollars')) {
                                    series('balance')
                                }
                                gridPane(id: 'table', hgap: 24, vgap: 4)
                            }
                        }
                        tab('Expense') {
                            gridPane(hgap: 8, vgap: 10, padding: 16) {
                                label('What for', row: 0, column: 0)
                                textField(id: 'description', promptText: 'Dinner', row: 0, column: 1, hgrow: 'ALWAYS')
                                label('Paid by', row: 1, column: 0)
                                choiceBox(id: 'payer', items: names, value: names[0], row: 1, column: 1)
                                label('Amount', row: 2, column: 0)
                                textField(id: 'amount', promptText: '0.00', row: 2, column: 1)
                                label('Split among', row: 3, column: 0)
                                flowPane(id: 'participants', hgap: 12, row: 3, column: 1) {
                                    names.each { checkBox(it, selected: true) }
                                }
                                button('Add expense', id: 'addExpense', row: 4, column: 1, onAction: {
                                    def chosen = fx.participants.children.findAll { ((CheckBox) it).selected }.collect { indexOf(((CheckBox) it).text) }
                                    act('Expense recorded') {
                                        group + group.splitEqually(fx.description.text ?: 'Expense', indexOf(fx.payer.value), amountOf(fx.amount.text), chosen)
                                    }
                                })
                            }
                        }
                        tab('Settle') {
                            gridPane(hgap: 8, vgap: 10, padding: 16) {
                                label('From', row: 0, column: 0)
                                choiceBox(id: 'from', items: names, value: names[0], row: 0, column: 1)
                                label('To', row: 1, column: 0)
                                choiceBox(id: 'to', items: names, value: names[1], row: 1, column: 1)
                                label('Amount', row: 2, column: 0)
                                textField(id: 'paid', promptText: '0.00', row: 2, column: 1)
                                button('Record payment', id: 'recordPayment', row: 3, column: 1, onAction: {
                                    act('Payment recorded') {
                                        group + new Settlement(indexOf(fx.from.value), indexOf(fx.to.value), amountOf(fx.paid.text))
                                    }
                                })
                            }
                        }
                        tab('History') {
                            listView(id: 'history', placeholder: new Label('No transactions yet'))
                        }
                    }
                }
                bottom {
                    vbox(padding: [4, 16, 12, 16]) {
                        label(id: 'status')
                        label('Splitting and balances proved at compile time with groovy-verify.', textFill: 'gray')
                    }
                }
            }
        }
    }

    static int amountOf(String text) {
        Integer cents = Group.cents(text)
        if (!cents) throw new IllegalArgumentException('Enter an amount above zero')
        cents
    }

    /** Applies an action to the group, reporting success or why it was rejected. */
    void act(String done, Closure<Group> action) {
        try {
            group = action()
            fx.amount.text = fx.paid.text = fx.description.text = ''
            refresh(done)
        } catch (IllegalArgumentException e) {
            fx.status.textFill = javafx.scene.paint.Color.FIREBRICK
            fx.status.text = e.message
        }
    }

    void refresh(String message) {
        def names = group.members()
        int[] balances = group.balances()
        fx.status.textFill = javafx.scene.paint.Color.SEAGREEN
        fx.status.text = message

        def series = (XYChart.Series) fx.chart.data[0]
        series.data.setAll(names.withIndex().collect { name, m -> new XYChart.Data(name, balances[m] / 100) })

        fx.table.children.clear()
        names.eachWithIndex { String name, int m ->
            int bal = balances[m]
            fx.table.addRow(m, new Label(name), new Label(Group.money(bal)),
                new Label(bal > 0 ? 'is owed' : bal < 0 ? 'owes' : 'settled'))
        }
        fx.table.addRow(names.size(), new Label('Total spent'), new Label(Group.money(group.totalSpent())),
            new Label("balances sum to ${Group.money(balances.sum())}"))

        fx.history.items.setAll(
            group.expenses().collect { "Expense  ${it.description()}: ${names[it.payer()]} paid ${Group.money(it.amount())}, split ${it.shares().count { s -> s > 0 }} ways" } +
            group.settlements().collect { "Payment  ${names[it.from()]} paid ${names[it.to()]} ${Group.money(it.amount())}" })
    }
}
