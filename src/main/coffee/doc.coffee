$ = window.$

show_initial_view = (hdr_node, aux_data) ->
    console.log('initial', aux_data)

    hdr_node.children().hide()
    initial_frame = hdr_node.children('.initial')
    initial_frame.show()

    initial_frame.find('#title').text(aux_data.title)
    initial_frame.find('#enabler').click () ->
        aux_data.base = '50'
        aux_data.halflife = '5000'
        show_detailed_view(hdr_node, aux_data)


show_detailed_view = (hdr_node, aux_data) ->
    console.log('detailed', aux_data)
    hdr_node.children().hide()
    frame = hdr_node.children('.detailed')
    frame.show()

    base_input = frame.find('#base')
    halflife_input = frame.find('#halflife')
    saver_btn = frame.find('#saver')

    base_input.val(aux_data.base)
    halflife_input.val(aux_data.halflife)

    saver_btn.click () ->
        data =
            'id': get_query_params().id
            'adjusted_base': base_input.val()
            'adjusted_halflife': halflife_input.val()
        saver_btn.attr('disabled', 'disabled')
        $.post 'drive/doc', data, (updated_data) ->
            saver_btn.removeAttr('disabled')
            console.log(updated_data)


window.get_doc_info = (header_node) ->
    id = get_query_params().id
    if not id
        alert 'Empty doc id'
        return 0

    header_node.children().not('.loading').hide()

    $.post 'drive/doc', {id:id}, (data) ->
        $('<iframe/>').attr('src', data.view_link).insertAfter(header_node)

        if data.base and data.halflife
            show_detailed_view header_node,data
        else
            show_initial_view header_node,data

    
