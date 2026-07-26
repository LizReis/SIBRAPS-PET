import { ComponentFixture, TestBed } from '@angular/core/testing';

import { NovoAgendamento } from './novo-agendamento';

describe('NovoAgendamento', () => {
  let component: NovoAgendamento;
  let fixture: ComponentFixture<NovoAgendamento>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NovoAgendamento]
    })
    .compileComponents();

    fixture = TestBed.createComponent(NovoAgendamento);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
