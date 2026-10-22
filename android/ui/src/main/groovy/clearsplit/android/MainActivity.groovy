package clearsplit.android

import android.app.Activity
import android.content.SharedPreferences
import android.graphics.Color
import android.graphics.Typeface
import android.os.Build
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Spinner
import android.widget.TextView
import clearsplit.Expense
import clearsplit.Group
import clearsplit.Settlement

import static android.view.ViewGroup.LayoutParams.MATCH_PARENT
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT

/**
 * ClearSplit on Android: the same verified core as the Grails and GroovyFX apps, with the
 * screens built in code in dynamic Groovy. The group is saved to the activity's preferences
 * after every change and restored by replaying its history through {@link Group}, so a saved
 * group gets the same validation as one entered by hand.
 */
class MainActivity extends Activity {
    private static final List<String> TABS = ['Balances', 'Expense', 'Settle', 'History']
    private static final int OWED = Color.rgb(46, 139, 87), OWES = Color.rgb(178, 34, 34)

    private SharedPreferences prefs
    private Group group
    private String tab = TABS[0]
    private FrameLayout content
    private LinearLayout tabBar
    private TextView status
    private int pad

    @Override
    void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState)
        pad = (16 * resources.displayMetrics.density) as int
        prefs = getPreferences(MODE_PRIVATE)
        group = restore(prefs.getString('group', null))
        tab = savedInstanceState?.getString('tab') ?: tab
        group ? showMain() : showSetup()
    }

    @Override
    protected void onSaveInstanceState(Bundle state) {
        super.onSaveInstanceState(state)
        state.putString('tab', tab)
    }

    private void showSetup() {
        def error = new TextView(this).tap { textColor = OWES }
        def names = new EditText(this).tap {
            hint = 'Alice\nBob\nCarol'
            inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_CAP_WORDS
            minLines = 4
            gravity = Gravity.TOP | Gravity.START
        }
        def start = new Button(this).tap {
            text = 'Start'
            setOnClickListener {
                try {
                    group = Group.of(names.text.toString().split(/[,\n]/) as List<String>)
                    tab = TABS[0]
                    save()
                    showMain()
                } catch (IllegalArgumentException e) {
                    error.text = e.message
                }
            }
        }
        contentView = screen(column(
            heading('Start a group'),
            label('Who is sharing expenses? One name per line, or separated by commas.'),
            names, error, start,
            footnote()))
    }

    private void showMain() {
        status = new TextView(this).tap { setPadding(0, pad.intdiv(2), 0, 0) }
        tabBar = new LinearLayout(this).tap { orientation = LinearLayout.HORIZONTAL }
        TABS.each { name ->
            tabBar.addView(new Button(this, null, android.R.attr.buttonBarButtonStyle).tap {
                text = name
                tag = name
                setOnClickListener { show(name) }
            }, new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
        }
        content = new FrameLayout(this)
        def newGroup = new Button(this, null, android.R.attr.buttonBarButtonStyle).tap {
            text = 'New group'
            setOnLongClickListener { group = null; prefs.edit().remove('group').apply(); showSetup(); true }
            setOnClickListener { say('Long-press to start a new group', OWES) }
        }
        def header = new LinearLayout(this).tap {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            addView(column(heading('ClearSplit'), label(group.members().join(' / '), Color.GRAY)),
                new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1f))
            addView(newGroup)
        }
        contentView = screen(column(header, tabBar, content, status, footnote()))
        show(tab)
    }

    private void show(String name, String message = '', int colour = OWED) {
        tab = name
        tabBar.childCount.times { int i ->
            def button = tabBar.getChildAt(i) as Button
            button.typeface = button.tag == name ? Typeface.DEFAULT_BOLD : Typeface.DEFAULT
        }
        content.removeAllViews()
        content.addView(this."${name.toLowerCase()}Tab"())
        say(message, colour)
    }

    private void say(String message, int colour) {
        status.text = message
        status.textColor = colour
    }

    /** Applies an action to the group, staying on this tab and saying why when it's rejected. */
    private void act(String done, Closure<Group> action) {
        try {
            group = action()
            save()
            show(tab, done)
        } catch (IllegalArgumentException e) {
            say(e.message, OWES)
        }
    }

    private View balancesTab() {
        def names = group.members()
        int[] balances = group.balances()
        int biggest = Math.max(1, balances.collect { Math.abs(it) }.max())
        def rows = names.withIndex().collect { String name, int m ->
            int bal = balances[m]
            row([label(name), 3], [bar(bal, biggest), 4],
                [label(Group.money(bal), bal > 0 ? OWED : bal < 0 ? OWES : Color.GRAY).tap { gravity = Gravity.END | Gravity.CENTER_VERTICAL }, 3])
        }
        column(*rows,
            row([label('Total spent').tap { typeface = Typeface.DEFAULT_BOLD }, 7],
                [label(Group.money(group.totalSpent())).tap { gravity = Gravity.END | Gravity.CENTER_VERTICAL; typeface = Typeface.DEFAULT_BOLD }, 3]),
            label("Balances sum to ${Group.money(balances.sum())}", Color.GRAY))
    }

    private View expenseTab() {
        def names = group.members()
        def description = field('What for (e.g. Dinner)', InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES)
        def payer = spinner(names, 0)
        def amount = field('Amount, e.g. 12.50', InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL)
        def boxes = names.collect { name -> new CheckBox(this).tap { text = name; checked = true } }
        def participants = new LinearLayout(this).tap {
            orientation = LinearLayout.HORIZONTAL
            boxes.each { addView(it) }
        }
        column(description, row([label('Paid by'), 3], [payer, 7]), amount,
            label('Split equally among'), new android.widget.HorizontalScrollView(this).tap { addView(participants) },
            new Button(this).tap {
                text = 'Add expense'
                setOnClickListener {
                    def chosen = (0..<boxes.size()).findAll { boxes[it].checked }
                    act('Expense recorded') {
                        group + group.splitEqually(description.text.toString().trim() ?: 'Expense',
                            payer.selectedItemPosition, amountOf(amount), chosen)
                    }
                }
            })
    }

    private View settleTab() {
        def names = group.members()
        def from = spinner(names, 0)
        def to = spinner(names, 1)
        def amount = field('Amount, e.g. 20', InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL)
        column(row([label('From'), 3], [from, 7]), row([label('To'), 3], [to, 7]), amount,
            new Button(this).tap {
                text = 'Record payment'
                setOnClickListener {
                    act('Payment recorded') {
                        group + new Settlement(from.selectedItemPosition, to.selectedItemPosition, amountOf(amount))
                    }
                }
            })
    }

    private View historyTab() {
        def names = group.members()
        def lines = group.expenses().collect { Expense e ->
            "${e.description()}: ${names[e.payer()]} paid ${Group.money(e.amount())}, split ${e.shares().count { it > 0 }} ways"
        } + group.settlements().collect { Settlement s ->
            "${names[s.from()]} paid ${names[s.to()]} ${Group.money(s.amount())}"
        }
        lines ? column(*lines.collect { label(it).tap { setPadding(0, pad.intdiv(4), 0, pad.intdiv(4)) } })
              : label('No transactions yet', Color.GRAY)
    }

    private static int amountOf(EditText field) {
        Integer cents = Group.cents(field.text.toString())
        if (!cents) throw new IllegalArgumentException('Enter an amount above zero')
        cents
    }

    // ---- saving: the members, then one line per expense or settlement, replayed through Group

    private void save() {
        prefs.edit().putString('group', group ? write(group) : null).apply()
    }

    static String write(Group group) {
        ([group.members().join('\t')] +
            group.expenses().collect { Expense e -> ['E', e.payer(), e.amount(), e.shares().join(','), e.description().replaceAll(/\s/, ' ')].join('\t') } +
            group.settlements().collect { Settlement s -> ['S', s.from(), s.to(), s.amount()].join('\t') }).join('\n')
    }

    static Group restore(String saved) {
        if (!saved) return null
        try {
            def lines = saved.readLines()
            lines.drop(1).inject(Group.of(lines[0].split('\t') as List<String>)) { Group g, String line ->
                def f = line.split('\t', -1)
                f[0] == 'E' ? g + new Expense(f[4], f[1] as int, f[2] as int, f[3].split(',').collect { it as Integer })
                            : g + new Settlement(f[1] as int, f[2] as int, f[3] as int)
            }
        } catch (RuntimeException ignored) {
            null   // an unreadable save starts afresh rather than crashing
        }
    }

    // ---- view helpers

    /** A bar from the middle: green to the right for what a member is owed, red to the left for what they owe. */
    private View bar(int balance, int biggest) {
        float filled = (Math.abs(balance) / biggest) as float
        def half = { boolean active, int colour, int side ->
            new LinearLayout(this).tap {
                orientation = LinearLayout.HORIZONTAL
                gravity = side
                weightSum = 1f
                if (active) addView(new View(this).tap { backgroundColor = colour },
                    new LinearLayout.LayoutParams(0, MATCH_PARENT, filled))
            }
        }
        row([half(balance < 0, OWES, Gravity.END), 1], [half(balance > 0, OWED, Gravity.START), 1]).tap {
            setPadding(pad.intdiv(4), pad.intdiv(2) + pad.intdiv(4), pad.intdiv(4), pad.intdiv(2) + pad.intdiv(4))
        }
    }

    private LinearLayout row(List... cells) {
        new LinearLayout(this).tap {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = pad * 3
            cells.each { cell -> addView(cell[0] as View, new LinearLayout.LayoutParams(0, MATCH_PARENT, cell[1] as float)) }
        }
    }

    private LinearLayout column(View... views) {
        new LinearLayout(this).tap {
            orientation = LinearLayout.VERTICAL
            views.each { addView(it, new LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)) }
        }
    }

    private TextView heading(String text) {
        label(text).tap { textSize = 24f; typeface = Typeface.DEFAULT_BOLD }
    }

    private TextView label(String text, int colour = Color.DKGRAY) {
        new TextView(this).tap {
            setText(text)
            textColor = colour
            textSize = 16f
            gravity = Gravity.CENTER_VERTICAL
        }
    }

    private TextView footnote() {
        label("Splitting and balances proved at compile time with groovy-verify.\nGroovy ${GroovySystem.version} on Android ${Build.VERSION.RELEASE}", Color.GRAY).tap {
            textSize = 12f
            setPadding(0, pad, 0, 0)
        }
    }

    private EditText field(String hint, int type) {
        new EditText(this).tap { setHint(hint); inputType = type; singleLine = true }
    }

    private Spinner spinner(List<String> items, int selected) {
        new Spinner(this).tap {
            adapter = new ArrayAdapter<String>(this, android.R.layout.simple_spinner_dropdown_item, items)
            setSelection(selected)
        }
    }

    /** Scrolls, with the padding inside a wrapper that takes the system-bar insets (targetSdk 35+ is edge to edge). */
    private View screen(View body) {
        new FrameLayout(this).tap {
            fitsSystemWindows = true
            addView(new ScrollView(this).tap {
                addView(body.tap { setPadding(pad, pad, pad, pad) })
            })
        }
    }
}
