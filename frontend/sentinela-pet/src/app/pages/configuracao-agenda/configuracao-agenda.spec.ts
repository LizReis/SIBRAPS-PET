import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ConfiguracaoAgenda } from './configuracao-agenda';

describe('ConfiguracaoAgenda', () => {
  let component: ConfiguracaoAgenda;
  let fixture: ComponentFixture<ConfiguracaoAgenda>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ConfiguracaoAgenda]
    })
    .compileComponents();

    fixture = TestBed.createComponent(ConfiguracaoAgenda);
    component = fixture.componentInstance;
    await fixture.whenStable();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
