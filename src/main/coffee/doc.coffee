$ = window.$

window.get_doc_info = (header_node) ->
    id = get_query_params().id
    if not id
        alert 'Empty doc id'
        return 0

    $.post 'drive/doc', {id:id}, (data) ->
        header_node.find('h5').text(data.title)
        $('<iframe/>').attr('src', data.view_link).insertAfter(header_node)
