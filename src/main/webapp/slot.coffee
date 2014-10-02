window.make_signal_binder = (name) ->
    slots = []
    {
        add_handler: (h) ->
            slots.push(h)
        invoke: (args...) ->
            console.log("SIGNAL: #{name}: #{args}")
            for sl in slots
                sl args...
    }
