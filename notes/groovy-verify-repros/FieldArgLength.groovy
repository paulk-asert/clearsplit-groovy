package clearsplit

import groovy.contracts.Ensures
import groovy.contracts.Invariant
import groovy.contracts.Requires
import groovy.transform.TypeChecked

@TypeChecked(extensions = 'verification.VerifyChecker')
@Invariant({ n >= 0 && data != null && data.length == n })
class LabD {
    int n
    int[] data

    @Requires({ a != null && 0 <= i && i < a.length })
    @Ensures({ result.length == a.length })
    static int[] bump(int[] a, int i) { int[] r = new int[a.length]; r }

    @Requires({ 0 <= i && i < n })
    void direct(int i) { data = bump(data, i) }

    @Requires({ 0 <= i && i < n })
    void viaThis(int i) { data = bump(this.data, i) }

    @Requires({ 0 <= i && i < n })
    void viaLocal(int i) { int[] d = data; data = bump(d, i) }
}
