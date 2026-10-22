package clearsplit

/**
 * One page for a group: balances, adding an expense or settlement, and the history.
 * The group lives in the HTTP session, so each browser has its own.
 */
class SplitController {

    static allowedMethods = [start: 'POST', expense: 'POST', settle: 'POST', reset: 'POST']

    def index() {
        Group group = session.group as Group
        if (!group) return render(view: 'setup')
        int[] balances = group.balances()
        [group: group, balances: balances, tab: params.tab ?: 'balances']
    }

    def start(String members) {
        attempt('balances') {
            session.group = Group.of((members ?: '').split(/[,\n]/) as List<String>)
            null
        }
    }

    def expense(String description, Integer payer, String amount) {
        attempt('expense') {
            Group group = session.group as Group
            Integer cents = Group.cents(amount)
            if (!cents) throw new IllegalArgumentException('Enter an amount above zero')
            List<Integer> participants = params.list('participant').collect { it as Integer }
            session.group = group + group.splitEqually(description ?: 'Expense', payer ?: 0, cents, participants)
            'Expense recorded'
        }
    }

    def settle(Integer from, Integer to, String amount) {
        attempt('settle') {
            Group group = session.group as Group
            Integer cents = Group.cents(amount)
            if (!cents) throw new IllegalArgumentException('Enter an amount above zero')
            session.group = group + new Settlement(from ?: 0, to ?: 0, cents)
            'Payment recorded'
        }
    }

    def reset() {
        session.removeAttribute('group')
        redirect action: 'index'
    }

    private attempt(String tab, Closure<String> action) {
        try {
            flash.message = action()
            redirect action: 'index', params: [tab: flash.message ? 'balances' : tab]
        } catch (IllegalArgumentException e) {
            flash.error = e.message
            redirect action: 'index', params: [tab: tab]
        }
    }
}
