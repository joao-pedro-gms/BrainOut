// João Pedro G M Silva - PUC Goiás ADS - 20251012000740
// Identificadores de teste Compose compartilhados entre a tela e os
// composables extraídos em :feature:projects/ui/projectdetail. Objeto
// interno no pacote — visível para os componentes extraídos, opaco
// para outros módulos.

package pucgo.joaopedrogmsilva.brainout.feature.projects.ui.projectdetail

object ProjectDetailTestTags {
    const val NEW_TASK_FAB: String = "project_detail_new_task_fab"
    const val DELETE_PROJECT_BUTTON: String = "project_detail_delete_project_button"
    const val NEW_TASK_DIALOG: String = "project_detail_new_task_dialog"
    const val NEW_TASK_TITLE_FIELD: String = "project_detail_new_task_title"
    const val NEW_TASK_SAVE: String = "project_detail_new_task_save"
    const val NEW_TASK_CANCEL: String = "project_detail_new_task_cancel"
    const val NEW_TASK_DUE_DATE_FIELD: String = "project_detail_new_task_due_date_field"
    const val NEW_TASK_DUE_DATE_FIELD_OVERLAY: String = "project_detail_new_task_due_date_overlay"
    const val NEW_TASK_HOLIDAY_HINT: String = "project_detail_new_task_holiday_hint"
    const val TASK_ITEM_MENU: String = "project_detail_task_item_menu"
    const val TASK_ITEM_MENU_DELETE: String = "project_detail_task_item_menu_delete"
    const val TASK_ITEM_MENU_MOVE_DOING: String = "project_detail_task_item_menu_move_doing"
    const val TASK_ITEM_MENU_MOVE_DONE: String = "project_detail_task_item_menu_move_done"
    const val TASK_ITEM_MENU_MOVE_TODO: String = "project_detail_task_item_menu_move_todo"
    const val TASK_ITEM_MENU_CHANGE_PRIORITY: String = "project_detail_task_item_menu_change_priority"
    const val TASK_PRIORITY_DIALOG: String = "project_detail_task_priority_dialog"
    const val TASK_PRIORITY_CHIP_OPTION_PREFIX: String = "project_detail_task_priority_option_"
    // E2.8 — tags de teste para loader e banner de erro.
    const val LOADING: String = "project_detail_loading"
    const val ERROR_BANNER: String = "project_detail_error_banner"
    const val ERROR_RETRY: String = "project_detail_error_retry"
    const val ERROR_DISMISS: String = "project_detail_error_dismiss"

    // E3.4 — banner offline + contagem da fila de sincronização.
    const val OFFLINE_BANNER: String = "project_detail_offline_banner"
    const val TASK_LIST: String = "project_detail_task_list"
}
