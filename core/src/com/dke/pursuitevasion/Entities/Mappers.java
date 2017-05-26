package com.dke.pursuitevasion.Entities;

import com.badlogic.ashley.core.ComponentMapper;
import com.dke.pursuitevasion.Entities.Components.*;
import com.dke.pursuitevasion.Entities.Components.agents.CCTvComponent;
import com.dke.pursuitevasion.Entities.Components.agents.PursuerComponent;

/**
 * Created by Nicola Gheza on 08/05/2017.
 */
public class Mappers {
    public static ComponentMapper<StateComponent> stateMapper = ComponentMapper.getFor(StateComponent.class);
    public static ComponentMapper<AgentComponent> agentMapper = ComponentMapper.getFor(AgentComponent.class);
    public static ComponentMapper<WallComponent> wallMapper = ComponentMapper.getFor(WallComponent.class);
    public static ComponentMapper<GraphicsComponent> graphicsMapper = ComponentMapper.getFor(GraphicsComponent.class);

    public static ComponentMapper<ObservableComponent> observableMapper = ComponentMapper.getFor(ObservableComponent.class);
    public static ComponentMapper<ObserverComponent> observerMapper = ComponentMapper.getFor(ObserverComponent.class);

    public static ComponentMapper<CCTvComponent> cctvMapper = ComponentMapper.getFor(CCTvComponent.class);
    public static ComponentMapper<PursuerComponent> pursuerMapper = ComponentMapper.getFor(PursuerComponent.class);
}
