// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
package pucgo.joaopedrogmsilva.brainout.core.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider

/**
 * O banco em memória dos testes que precisam do Room de verdade.
 *
 * A montagem estava copiada em sete suítes de quatro pacotes — o mesmo
 * `inMemoryDatabaseBuilder` com `allowMainThreadQueries` — e qualquer
 * ajuste no schema ou no executor do Room exigia sete edições. Aqui ela
 * mora uma vez.
 *
 * O contexto é sempre o da aplicação de teste: nas suítes que já
 * guardavam um `context` próprio (as de backup, por exemplo), ele
 * vinha da mesma [ApplicationProvider], então o banco é idêntico ao
 * que a suíte construía antes.
 */
internal fun inMemoryDatabase(): BrainOutDatabase {
    val context = ApplicationProvider.getApplicationContext<Context>()
    return Room
        .inMemoryDatabaseBuilder(context, BrainOutDatabase::class.java)
        .allowMainThreadQueries()
        .build()
}
